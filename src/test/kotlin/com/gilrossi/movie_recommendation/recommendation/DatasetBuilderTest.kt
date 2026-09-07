package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.ContentType
import com.gilrossi.movie_recommendation.model.Rating
import com.gilrossi.movie_recommendation.model.RatingTargetType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class DatasetBuilderTest {
    @Test
    fun `uses direct content ratings as labels and excludes current label from history`() {
        val normalizer = RatingNormalizer()
        val builder = RecommendationDatasetBuilder(FeatureBuilder(normalizer), normalizer)
        val content = Content(7, "Movie", null, ContentType.MOVIE, null, null, null)
        val rating = Rating(1, 1, RatingTargetType.MOVIE, 5, 7, null, null, Instant.EPOCH, Instant.EPOCH)

        val dataset = builder.build(listOf(rating), listOf(ContentSignals(content, emptySet(), emptySet(), emptySet())))

        assertEquals(1, dataset.size)
        assertEquals(1f, dataset.single().label)
        assertEquals(0.5f, dataset.single().features[FeatureVector.HISTORY])
    }
}
