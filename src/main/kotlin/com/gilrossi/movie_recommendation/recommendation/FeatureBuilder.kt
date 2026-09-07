package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.model.ContentType
import com.gilrossi.movie_recommendation.model.Rating
import com.gilrossi.movie_recommendation.model.RatingTargetType
import org.springframework.stereotype.Component

@Component
class FeatureBuilder(private val normalizer: RatingNormalizer) {
    fun build(
        ratings: List<Rating>,
        signals: ContentSignals,
        popularity: Double = NEUTRAL,
        vectorSimilarity: Double = NEUTRAL
    ): FeatureVector {
        val genre = affinity(ratings, RatingTargetType.GENRE, signals.genreIds)
        val actor = affinity(ratings, RatingTargetType.ACTOR, signals.actorIds)
        val role = if (signals.content.type == ContentType.MOVIE) RatingTargetType.DIRECTOR else RatingTargetType.CREATOR
        val director = affinity(ratings, role, signals.directorOrCreatorIds)
        val movie = average(ratings.filter { it.targetType == RatingTargetType.MOVIE })
        val series = average(ratings.filter { it.targetType == RatingTargetType.SERIES })
        val history = average(ratings.filter { it.targetType.isContent })
        val values = floatArrayOf(
            genre.toFloat(), actor.toFloat(), director.toFloat(), movie.toFloat(), series.toFloat(),
            history.toFloat(), popularity.coerceIn(0.0, 1.0).toFloat(), vectorSimilarity.coerceIn(0.0, 1.0).toFloat()
        )
        val explanations = buildList {
            if (genre > POSITIVE) add("Afinidade com os gêneros (${percent(genre)})")
            if (actor > POSITIVE) add("Atores bem avaliados (${percent(actor)})")
            if (director > POSITIVE) add("${if (role == RatingTargetType.CREATOR) "Creators" else "Diretores"} bem avaliados (${percent(director)})")
            if (vectorSimilarity > POSITIVE) add("Conteúdo semelhante ao perfil (${percent(vectorSimilarity)})")
            if (isEmpty()) add("Compatível com o histórico geral")
        }
        return FeatureVector(values, explanations)
    }

    private fun affinity(ratings: List<Rating>, type: RatingTargetType, ids: Set<Long>): Double =
        average(ratings.filter { it.targetType == type && it.targetId() in ids })

    private fun average(ratings: List<Rating>): Double = ratings
        .takeIf { it.isNotEmpty() }
        ?.map { normalizer.normalize(it.value) }
        ?.average()
        ?: NEUTRAL

    private fun percent(value: Double) = "${(value * 100).toInt()}%"

    companion object {
        const val NEUTRAL = 0.5
        private const val POSITIVE = 0.55
    }
}
