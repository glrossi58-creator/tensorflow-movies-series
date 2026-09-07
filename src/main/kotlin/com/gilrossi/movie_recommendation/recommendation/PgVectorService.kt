package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.model.Rating
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import kotlin.math.abs

@Service
class PgVectorService(
    private val databaseClient: DatabaseClient,
    private val generator: ContentEmbeddingGenerator,
    private val normalizer: RatingNormalizer
) {
    suspend fun refreshContents(signals: Collection<ContentSignals>) {
        signals.forEach { saveContentEmbedding(requireNotNull(it.content.id), generator.generate(it)) }
    }

    suspend fun refreshUser(userId: Long, ratings: List<Rating>, signals: Collection<ContentSignals>) {
        val byId = signals.associateBy { it.content.id }
        val directRatings = ratings.filter { it.targetType.isContent && it.contentId in byId }
        if (directRatings.isEmpty()) return

        val result = FloatArray(ContentEmbeddingGenerator.DIMENSIONS)
        var totalWeight = 0.0
        directRatings.forEach { rating ->
            val weight = (normalizer.normalize(rating.value) - 0.5) * 2.0
            val embedding = generator.generate(requireNotNull(byId[rating.contentId]))
            result.indices.forEach { result[it] += (embedding[it] * weight).toFloat() }
            totalWeight += abs(weight)
        }
        if (totalWeight == 0.0) return
        result.indices.forEach { result[it] = (result[it] / totalWeight).toFloat() }
        saveUserEmbedding(userId, result)
    }

    suspend fun similarities(userId: Long): Map<Long, Double> = databaseClient.sql(
        """
        SELECT c.id, GREATEST(0.0, LEAST(1.0, 1.0 - (c.embedding <=> u.embedding))) AS similarity
        FROM content c CROSS JOIN app_user u
        WHERE u.id = :userId AND c.embedding IS NOT NULL AND u.embedding IS NOT NULL
        """.trimIndent()
    )
        .bind("userId", userId)
        .map { row, _ ->
            requireNotNull(row.get("id", Long::class.javaObjectType)) to
                requireNotNull(row.get("similarity", Double::class.javaObjectType))
        }
        .all().collectList().awaitSingleOrNull().orEmpty().toMap()

    suspend fun saveContentEmbedding(contentId: Long, embedding: FloatArray) {
        databaseClient.sql("UPDATE content SET embedding = CAST(:embedding AS vector) WHERE id = :id")
            .bind("embedding", embedding.toVectorLiteral())
            .bind("id", contentId)
            .fetch().rowsUpdated().awaitSingleOrNull()
    }

    private suspend fun saveUserEmbedding(userId: Long, embedding: FloatArray) {
        databaseClient.sql("UPDATE app_user SET embedding = CAST(:embedding AS vector), updated_at = now() WHERE id = :id")
            .bind("embedding", embedding.toVectorLiteral())
            .bind("id", userId)
            .fetch().rowsUpdated().awaitSingleOrNull()
    }

    private fun FloatArray.toVectorLiteral(): String = joinToString(prefix = "[", postfix = "]")
}
