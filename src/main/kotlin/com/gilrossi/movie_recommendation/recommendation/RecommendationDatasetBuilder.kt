package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.model.Rating
import org.springframework.stereotype.Component

@Component
class RecommendationDatasetBuilder(
    private val featureBuilder: FeatureBuilder,
    private val normalizer: RatingNormalizer
) {
    /** Split before computing any history-based feature: validation labels never enter training features. */
    fun split(ratings: List<Rating>, signals: Collection<ContentSignals>, otherUserPopularity: Map<Long, Double>): Pair<List<LabeledExample>, List<LabeledExample>> {
        val byId = signals.associateBy { it.content.id }
        val labels = ratings.filter { it.targetType.isContent && it.contentId in byId }.shuffled(kotlin.random.Random(42))
        val validation = labels.take(maxOf(1, labels.size / 5))
        val training = labels.drop(validation.size)
        val preferences = ratings.filterNot { it.targetType.isContent }
        val embedding = UserEmbeddingBuilder(ContentEmbeddingGenerator(), normalizer)
        fun examples(selected: List<Rating>, isTraining: Boolean) = selected.map { label ->
            val history = preferences + if (isTraining) training.filterNot { it.contentId == label.contentId } else training
            val signal = byId.getValue(label.contentId)
            val features = featureBuilder.build(history, signal, otherUserPopularity[label.contentId] ?: 0.5,
                embedding.similarity(history, signals, signal))
            LabeledExample(label.contentId!!, features.values, normalizer.normalize(label.value).toFloat())
        }
        return examples(training, true) to examples(validation, false)
    }
    fun build(
        ratings: List<Rating>,
        signals: Collection<ContentSignals>,
        popularity: Map<Long, Double> = emptyMap(),
        vectorSimilarities: Map<Long, Double> = emptyMap()
    ): List<LabeledExample> {
        val signalsById = signals.associateBy { it.content.id }
        return ratings.filter { it.targetType.isContent && it.contentId in signalsById }.map { labelRating ->
            val contentId = requireNotNull(labelRating.contentId)
            val historyWithoutLabel = ratings.filterNot { it.targetType.isContent && it.contentId == labelRating.contentId }
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
