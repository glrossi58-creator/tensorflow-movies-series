package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.model.Content

data class ContentSignals(
    val content: Content,
    val genreIds: Set<Long>,
    val actorIds: Set<Long>,
    val directorOrCreatorIds: Set<Long>
)

data class FeatureVector(
    val values: FloatArray,
    val explanations: List<String>
) {
    init { require(values.size == FEATURE_COUNT) }
    companion object {
        const val FEATURE_COUNT = 8
        const val GENRE = 0
        const val ACTOR = 1
        const val DIRECTOR = 2
        const val MOVIE = 3
        const val SERIES = 4
        const val HISTORY = 5
        const val POPULARITY = 6
        const val VECTOR = 7
    }
}

data class LabeledExample(val contentId: Long, val features: FloatArray, val label: Float)

data class RecommendationResult(
    val content: Content,
    val score: Double,
    val strategy: String,
    val reasons: List<String>
)

data class JointRecommendationResult(
    val content: Content,
    val score: Double,
    val individualScores: Map<Long, Double>,
    val reasons: List<String>
)
