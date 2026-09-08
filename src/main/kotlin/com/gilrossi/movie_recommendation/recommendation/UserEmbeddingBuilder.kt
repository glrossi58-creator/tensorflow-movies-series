package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.model.ContentType
import com.gilrossi.movie_recommendation.model.Rating
import com.gilrossi.movie_recommendation.model.RatingTargetType
import org.springframework.stereotype.Component
import kotlin.math.abs
import kotlin.math.sqrt

@Component
class UserEmbeddingBuilder(private val generator: ContentEmbeddingGenerator, private val normalizer: RatingNormalizer) {
    fun build(ratings: List<Rating>, signals: Collection<ContentSignals>): FloatArray? {
        val result = FloatArray(8)
        var total = 0.0
        ratings.forEach { rating ->
            val related = signals.filter { s -> when (rating.targetType) {
                RatingTargetType.MOVIE, RatingTargetType.SERIES -> s.content.id == rating.contentId
                RatingTargetType.GENRE -> rating.genreId in s.genreIds
                RatingTargetType.ACTOR -> rating.personId in s.actorIds
                RatingTargetType.DIRECTOR -> s.content.type == ContentType.MOVIE && rating.personId in s.directorOrCreatorIds
                RatingTargetType.CREATOR -> s.content.type == ContentType.SERIES && rating.personId in s.directorOrCreatorIds
            } }
            if (related.isNotEmpty()) {
                val weight = (normalizer.normalize(rating.value) - 0.5) * 2.0 / related.size
                related.forEach { s ->
                    val vector = generator.generate(s)
                    result.indices.forEach { result[it] += (vector[it] * weight).toFloat() }
                    total += abs(weight)
                }
            }
        }
        if (total == 0.0) return null
        val norm = sqrt(result.sumOf { it.toDouble() * it })
        if (norm < 1e-8) return null
        return FloatArray(8) { (result[it] / norm).toFloat() }
    }

    fun similarity(ratings: List<Rating>, history: Collection<ContentSignals>, candidate: ContentSignals): Double {
        val profile = build(ratings, history) ?: return 0.5
        val vector = generator.generate(candidate)
        return profile.indices.sumOf { profile[it].toDouble() * vector[it] }.coerceIn(0.0, 1.0)
    }
}
