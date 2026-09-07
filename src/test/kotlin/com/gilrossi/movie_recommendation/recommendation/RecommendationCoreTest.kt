package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.ContentType
import com.gilrossi.movie_recommendation.model.Rating
import com.gilrossi.movie_recommendation.model.RatingTargetType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class RecommendationCoreTest {
    private val normalizer = RatingNormalizer()

    @Test
    fun `normalizes complete rating scale`() {
        assertEquals(listOf(0.0, 0.25, 0.5, 0.75, 1.0), (1..5).map(normalizer::normalize))
    }

    @Test
    fun `builds semantic features without mixing person roles`() {
        val builder = FeatureBuilder(normalizer)
        val ratings = listOf(
            rating(1, RatingTargetType.GENRE, 10, 5),
            rating(2, RatingTargetType.ACTOR, 20, 5),
            rating(3, RatingTargetType.DIRECTOR, 30, 1),
            rating(4, RatingTargetType.CREATOR, 30, 5)
        )
        val movie = ContentSignals(content(ContentType.MOVIE), setOf(10), setOf(20), setOf(30))
        val features = builder.build(ratings, movie)

        assertEquals(1f, features.values[FeatureVector.GENRE])
        assertEquals(1f, features.values[FeatureVector.ACTOR])
        assertEquals(0f, features.values[FeatureVector.DIRECTOR])
    }

    @Test
    fun `baseline weights are configurable`() {
        val weights = BaselineWeights().apply { genre = 1.0; actor = 0.0; director = 0.0; movie = 0.0; series = 0.0 }
        val score = BaselineRecommender(weights).score(FeatureVector(floatArrayOf(0.8f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), emptyList()))
        assertEquals(0.8, score, 0.0001)
    }

    @Test
    fun `joint score penalizes a bad experience for one participant`() {
        val aggregator = JointScoreAggregator()
        assertTrue(aggregator.aggregate(listOf(1.0, 0.1)) < aggregator.aggregate(listOf(0.7, 0.7)))
    }

    private fun content(type: ContentType) = Content(99, "Content", null, type, null, null, null)

    private fun rating(id: Long, type: RatingTargetType, targetId: Long, value: Int): Rating = Rating(
        id, 1, type, value,
        targetId.takeIf { type.isContent }, targetId.takeIf { type.isPerson }, targetId.takeIf { type == RatingTargetType.GENRE },
        Instant.EPOCH, Instant.EPOCH
    )
}
