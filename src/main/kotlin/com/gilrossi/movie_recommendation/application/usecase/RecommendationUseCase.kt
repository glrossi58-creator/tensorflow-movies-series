package com.gilrossi.movie_recommendation.application.usecase

import com.gilrossi.movie_recommendation.exception.UserNotFoundException
import com.gilrossi.movie_recommendation.model.Rating
import com.gilrossi.movie_recommendation.recommendation.BaselineRecommender
import com.gilrossi.movie_recommendation.recommendation.FeatureBuilder
import com.gilrossi.movie_recommendation.recommendation.JointRecommendationResult
import com.gilrossi.movie_recommendation.recommendation.JointScoreAggregator
import com.gilrossi.movie_recommendation.recommendation.PgVectorService
import com.gilrossi.movie_recommendation.recommendation.RatingNormalizer
import com.gilrossi.movie_recommendation.recommendation.RecommendationDataService
import com.gilrossi.movie_recommendation.recommendation.RecommendationDatasetBuilder
import com.gilrossi.movie_recommendation.recommendation.RecommendationResult
import com.gilrossi.movie_recommendation.recommendation.TensorFlowRecommendationModel
import com.gilrossi.movie_recommendation.recommendation.TrainedRecommendationModel
import com.gilrossi.movie_recommendation.repository.AppUserRepository
import com.gilrossi.movie_recommendation.repository.RatingRepository
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class RecommendationUseCase(
    private val userRepository: AppUserRepository,
    private val ratingRepository: RatingRepository,
    private val dataService: RecommendationDataService,
    private val featureBuilder: FeatureBuilder,
    private val baseline: BaselineRecommender,
    private val datasetBuilder: RecommendationDatasetBuilder,
    private val tensorflow: TensorFlowRecommendationModel,
    private val pgVector: PgVectorService,
    private val normalizer: RatingNormalizer,
    private val jointScoreAggregator: JointScoreAggregator,
    @Value("\${recommendation.ml.minimum-samples:8}") private val minimumSamples: Int,
    @Value("\${recommendation.vector-weight:0.15}") private val vectorWeight: Double,
    private val models: com.gilrossi.movie_recommendation.recommendation.ModelLifecycleService,
    private val catalog: com.gilrossi.movie_recommendation.discovery.CatalogDiscoveryService
) {
    suspend fun recommend(userId: Long, limit: Int = 20): List<RecommendationResult> {
        require(limit in 1..100) { "limit deve estar entre 1 e 100." }
        return rank(userId).take(limit)
    }

    private suspend fun rank(userId: Long): List<RecommendationResult> {
        if (!userRepository.existsById(userId)) throw UserNotFoundException()
        catalog.ensureCandidates(listOf(userId))
        val context = prepare(userId)
        val directRatings = context.ratings.filter { it.targetType.isContent }
        val ratedContentIds = directRatings.mapNotNull(Rating::contentId).toSet()
        val model = models.trainedModel(userId)

        return context.signals.asSequence()
            .filterNot { it.content.id in ratedContentIds }
            .map { signals ->
                val contentId = requireNotNull(signals.content.id)
                val features = featureBuilder.build(
                    context.ratings, signals, context.popularity[contentId] ?: 0.5,
                    context.similarities[contentId] ?: 0.5
                )
                val baselineScore = baseline.score(features)
                val primary = model?.let { tensorflow.predict(features.values, it) } ?: baselineScore
                val vector = context.similarities[contentId]
                val blended = if (vector == null) primary else primary * (1.0 - vectorWeight) + vector * vectorWeight
                val coldStartAdjusted = if (context.ratings.isEmpty()) {
                    blended * 0.7 + (context.popularity[contentId] ?: 0.5) * 0.3
                } else blended
                RecommendationResult(
                    signals.content,
                    coldStartAdjusted.coerceIn(0.0, 1.0),
                    if (model == null) if (directRatings.isEmpty()) "BASELINE_COLD_START" else "BASELINE" else "TENSORFLOW",
                    features.explanations
                )
            }
            .sortedByDescending(RecommendationResult::score)
            .toList()
    }

    suspend fun train(userId: Long): TrainedRecommendationModel {
        return models.train(userId)
    }

    suspend fun refreshProfile(userId: Long) {
        prepare(userId)
    }

    suspend fun recommendTogether(userIds: List<Long>, limit: Int = 20): List<JointRecommendationResult> {
        val distinctIds = userIds.distinct()
        require(limit in 1..100) { "limit deve estar entre 1 e 100." }
        require(distinctIds.size >= 2) { "Informe ao menos dois usuários distintos." }
        require(distinctIds.size <= 20) { "No máximo 20 usuários por recomendação conjunta." }
        distinctIds.forEach { if (!userRepository.existsById(it)) throw UserNotFoundException() }
        catalog.ensureCandidates(distinctIds)
        val perUser = distinctIds.associateWith { rank(it).associateBy { recommendation -> recommendation.content.id } }
        val commonIds = perUser.values.map { it.keys }.reduce { common, ids -> common.intersect(ids) }

        return commonIds.map { contentId ->
            val entries = perUser.mapValues { (_, recommendations) -> requireNotNull(recommendations[contentId]) }
            val scores = entries.mapValues { it.value.score }
            val fairScore = jointScoreAggregator.aggregate(scores.values)
            val first = entries.values.first()
            JointRecommendationResult(
                first.content, fairScore.coerceIn(0.0, 1.0), scores,
                listOf("Equilíbrio entre ${scores.size} perfis", "Penaliza opções ruins para qualquer participante")
            )
        }.sortedByDescending(JointRecommendationResult::score).take(limit)
    }

    private suspend fun prepare(userId: Long): Context {
        if (!userRepository.existsById(userId)) throw UserNotFoundException()
        val ratings = ratingRepository.findAllByUserId(userId).toList()
        val signals = dataService.allSignals()
        pgVector.refreshContents(signals)
        pgVector.refreshUser(userId, ratings, signals)
        val similarities = pgVector.similarities(userId)
        val allRatings = ratingRepository.findAllByContentIdIsNotNull().toList()
        val popularity = allRatings.groupBy { it.contentId }.mapNotNull { (id, values) ->
            id?.let { it to values.map { rating -> normalizer.normalize(rating.value) }.average() }
        }.toMap()
        return Context(ratings, signals, popularity, similarities)
    }

    private data class Context(
        val ratings: List<Rating>,
        val signals: List<com.gilrossi.movie_recommendation.recommendation.ContentSignals>,
        val popularity: Map<Long, Double>,
        val similarities: Map<Long, Double>
    )
}
