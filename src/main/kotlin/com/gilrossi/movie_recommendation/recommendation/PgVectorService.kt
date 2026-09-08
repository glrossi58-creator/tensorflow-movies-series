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
        val result = UserEmbeddingBuilder(generator, normalizer).build(ratings, signals)
        if (result == null) {
            databaseClient.sql("UPDATE app_user SET embedding = NULL WHERE id = :id")
                .bind("id", userId).fetch().rowsUpdated().awaitSingleOrNull()
        } else saveUserEmbedding(userId, result)
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

    private fun FloatArray.toVectorLiteral(): String {
        require(size == 8 && all { it.isFinite() }) { "Embedding exige 8 valores finitos." }
        return joinToString(prefix = "[", postfix = "]")
    }
}
