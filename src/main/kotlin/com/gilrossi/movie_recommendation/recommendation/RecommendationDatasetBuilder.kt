package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.model.Rating
import org.springframework.stereotype.Component

@Component
class RecommendationDatasetBuilder(
    private val featureBuilder: FeatureBuilder,
    private val normalizer: RatingNormalizer
) {
    fun build(
        ratings: List<Rating>,
        signals: Collection<ContentSignals>,
        popularity: Map<Long, Double> = emptyMap(),
        vectorSimilarities: Map<Long, Double> = emptyMap()
    ): List<LabeledExample> {
        val signalsById = signals.associateBy { it.content.id }
        return ratings.filter { it.targetType.isContent && it.contentId in signalsById }.map { labelRating ->
            val contentId = requireNotNull(labelRating.contentId)
            val historyWithoutLabel = ratings.filterNot { it.id == labelRating.id }
            val features = featureBuilder.build(
                historyWithoutLabel,
                requireNotNull(signalsById[contentId]),
                popularity[contentId] ?: FeatureBuilder.NEUTRAL,
                vectorSimilarities[contentId] ?: FeatureBuilder.NEUTRAL
            )
            LabeledExample(contentId, features.values, normalizer.normalize(labelRating.value).toFloat())
        }
    }
}
