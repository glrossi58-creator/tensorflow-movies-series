package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.exception.ConflictException
import com.gilrossi.movie_recommendation.exception.UserNotFoundException
import com.gilrossi.movie_recommendation.repository.AppUserRepository
import com.gilrossi.movie_recommendation.repository.RatingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.time.Instant
import java.time.OffsetDateTime

data class ModelStatus(
    val userId: Long, val status: String, val directRatingCount: Int, val minimumDirectRatings: Int,
    val recommendedDirectRatings: Int, val ratingsUsedInLastTraining: Int, val newRatingsSinceLastTraining: Int,
    val canTrain: Boolean, val trainedAt: Instant?, val trainLoss: Double?, val validationLoss: Double?,
    val modelVersion: Int, val modelPath: String?, val autoTrainEnabled: Boolean, val lastError: String?,
    val revision: Long, val trainedRevision: Long
)

@Service
class ModelLifecycleService(
    private val db: DatabaseClient, private val users: AppUserRepository, private val ratings: RatingRepository,
    private val data: RecommendationDataService, private val datasets: RecommendationDatasetBuilder,
    private val tensorflow: TensorFlowRecommendationModel, private val normalizer: RatingNormalizer,
    @Value("\${recommendation.ml.minimum-samples:8}") private val minimum: Int,
    @Value("\${recommendation.ml.recommended-samples:25}") private val recommended: Int,
    @Value("\${recommendation.ml.auto-train-enabled:true}") private val autoEnabled: Boolean,
    @Value("\${recommendation.ml.auto-train-new-ratings:10}") private val autoThreshold: Int
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    init { require(minimum >= 2 && recommended >= minimum && autoThreshold > 0) }

    suspend fun status(userId: Long): ModelStatus {
        if (!users.existsById(userId)) throw UserNotFoundException()
        db.sql("INSERT INTO user_model_state(user_id) VALUES (:id) ON CONFLICT DO NOTHING").bind("id", userId).fetch().rowsUpdated().awaitSingle()
        val result = db.sql("""SELECT m.*,
            (SELECT count(*) FROM rating r WHERE r.user_id=m.user_id AND r.content_id IS NOT NULL) AS direct_count,
            (SELECT count(*) FROM rating r WHERE r.user_id=m.user_id AND r.content_id IS NOT NULL AND NOT EXISTS
                (SELECT 1 FROM user_model_sample s WHERE s.user_id=m.user_id AND s.model_version=m.model_version AND s.rating_id=r.id)) AS new_count
            FROM user_model_state m WHERE user_id=:id""").bind("id", userId).map { row, _ ->
                val count = (row.get("direct_count") as Number).toInt()
                val trainedAt = row.get("trained_at", OffsetDateTime::class.java)?.toInstant()
                val revision = (row.get("revision") as Number).toLong()
                val trainedRevision = (row.get("trained_revision") as Number).toLong()
                val stored = row.get("status", String::class.java)!!
                val status = when {
                    stored == "TRAINING" -> "TRAINING"
                    count < minimum -> "COLLECTING_DATA"
                    stored == "ERROR" -> "ERROR"
                    trainedAt == null -> "READY"
                    revision != trainedRevision -> "DIRTY"
                    else -> "TRAINED"
                }
                ModelStatus(userId, status, count, minimum, recommended,
                    (row.get("ratings_used_in_last_training") as Number).toInt(), (row.get("new_count") as Number).toInt(),
                    count >= minimum && status != "TRAINING", trainedAt,
                    (row.get("train_loss") as Number?)?.toDouble(), (row.get("validation_loss") as Number?)?.toDouble(),
                    (row.get("model_version") as Number).toInt(), row.get("model_path", String::class.java),
                    row.get("auto_train_enabled", java.lang.Boolean::class.java)?.booleanValue() ?: autoEnabled,
                    row.get("last_error", String::class.java), revision, trainedRevision)
            }.one().awaitSingle()
        db.sql("UPDATE user_model_state SET status=:status WHERE user_id=:id AND revision=:revision AND status <> 'TRAINING'")
            .bind("status", result.status).bind("id", userId).bind("revision", result.revision).fetch().rowsUpdated().awaitSingle()
        return result
    }

    suspend fun settings(userId: Long, enabled: Boolean): ModelStatus {
        status(userId)
        db.sql("UPDATE user_model_state SET auto_train_enabled=:enabled WHERE user_id=:id")
            .bind("enabled", enabled).bind("id", userId).fetch().rowsUpdated().awaitSingle()
        return status(userId)
    }

    suspend fun trainedModel(userId: Long): TrainedRecommendationModel? {
        val state = status(userId)
        val path = state.modelPath ?: return null
        return try {
            withContext(Dispatchers.IO) { tensorflow.loadFrom(Path.of(path)) }
                ?: throw IllegalStateException("Saved model is missing")
        }
        catch (e: Exception) {
            recordError(userId, "Não foi possível ler o modelo salvo. Treine novamente.")
            logger.warn("Cannot read saved model for user {}", userId)
            null
        }
    }

    suspend fun train(userId: Long): TrainedRecommendationModel {
        val before = status(userId)
        require(before.directRatingCount >= minimum) { "São necessárias ao menos $minimum avaliações de filmes/séries para treinar." }
        val acquired = db.sql("""UPDATE user_model_state SET status='TRAINING', training_started_at=now(), last_error=NULL
            WHERE user_id=:id AND status <> 'TRAINING'""").bind("id", userId).fetch().rowsUpdated().awaitSingle()
        if (acquired == 0L) throw ConflictException("O modelo deste perfil já está em treinamento.")
        try {
            // Capture revision before reading data. Concurrent changes keep the final state DIRTY.
            val snapshot = status(userId)
            val cutoff = Instant.now()
            val own = ratings.findAllByUserId(userId).toList()
            val signals = data.allSignals()
            val popularity = ratings.findAllByContentIdIsNotNull().toList().filter { it.userId != userId }
                .groupBy { it.contentId!! }.mapValues { (_, values) -> values.map { normalizer.normalize(it.value) }.average() }
            val (training, validation) = datasets.split(own, signals, popularity)
            require(training.size + validation.size >= minimum) { "A base de conteúdos disponível é insuficiente para treinar." }
            val model = withContext(Dispatchers.Default) { tensorflow.train(training, validation) }
            val version = snapshot.modelVersion + 1
            val path = tensorflow.versionPath(userId, version)
            withContext(Dispatchers.IO) { tensorflow.saveTo(path, model) }
            val included = (training + validation).map { it.contentId }.toSet()
            own.filter { it.targetType.isContent && it.contentId in included }.forEach { rating ->
                db.sql("INSERT INTO user_model_sample(user_id,model_version,rating_id) SELECT :user,:version,id FROM rating WHERE id=:rating ON CONFLICT DO NOTHING")
                    .bind("user", userId).bind("version", version).bind("rating", rating.id!!).fetch().rowsUpdated().awaitSingle()
            }
            db.sql("""UPDATE user_model_state SET
                status=CASE WHEN revision=:revision THEN 'TRAINED' ELSE 'DIRTY' END,
                trained_revision=:revision, ratings_used_in_last_training=:count, trained_at=now(), training_data_at=:cutoff,
                train_loss=:train, validation_loss=:validation, model_version=:version, model_path=:path,
                training_started_at=NULL, last_error=NULL WHERE user_id=:id""")
                .bind("revision", snapshot.revision).bind("count", training.size + validation.size).bind("cutoff", cutoff)
                .bind("train", model.trainingLoss).bind("validation", model.validationLoss).bind("version", version)
                .bind("path", path.toString()).bind("id", userId).fetch().rowsUpdated().awaitSingle()
            return model
        } catch (e: Exception) {
            withContext(kotlinx.coroutines.NonCancellable) { recordError(userId, "O treinamento falhou. Verifique o servidor e tente novamente.") }
            logger.error("Training failed for user {}", userId, e)
            throw e
        }
    }

    private suspend fun recordError(userId: Long, message: String) {
        db.sql("UPDATE user_model_state SET status='ERROR',last_error=:message,training_started_at=NULL WHERE user_id=:id")
            .bind("message", message).bind("id", userId).fetch().rowsUpdated().awaitSingle()
    }

    @Scheduled(fixedDelayString = "\${recommendation.ml.auto-check-ms:15000}", initialDelayString = "\${recommendation.ml.auto-check-ms:15000}")
    suspend fun autoTrain() {
        // Recover a lease abandoned by a process crash without allowing overlapping normal training.
        db.sql("""UPDATE user_model_state SET status='ERROR',last_error='Treinamento interrompido. Tente novamente.',training_started_at=NULL
            WHERE status='TRAINING' AND training_started_at < now() - interval '30 minutes'""").fetch().rowsUpdated().awaitSingle()
        val ids = db.sql("SELECT user_id FROM user_model_state WHERE status='DIRTY' AND trained_at IS NOT NULL")
            .map { row, _ -> (row.get("user_id") as Number).toLong() }.all().collectList().awaitSingle()
        ids.forEach { userId ->
            val state = status(userId)
            if (state.autoTrainEnabled && state.canTrain && state.newRatingsSinceLastTraining >= autoThreshold) {
                try { train(userId) } catch (_: ConflictException) { } catch (e: Exception) { logger.warn("Automatic training failed for user {}", userId) }
            }
        }
    }
}
