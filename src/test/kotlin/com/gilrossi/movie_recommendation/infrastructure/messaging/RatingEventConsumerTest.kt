package com.gilrossi.movie_recommendation.infrastructure.messaging

import com.gilrossi.movie_recommendation.application.usecase.RecommendationUseCase
import com.gilrossi.movie_recommendation.event.RatingEvent
import com.gilrossi.movie_recommendation.model.RatingTargetType
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RatingEventConsumerTest {
    @Test
    fun `consumer refreshes only affected user profile`() = runTest {
        val recommendations = mockk<RecommendationUseCase>(relaxed = true)
        val consumer = RatingEventConsumer(recommendations)
        val event = RatingEvent(UUID.randomUUID(), 1, 7, RatingTargetType.GENRE, 2, 5, Instant.EPOCH)

        consumer.consume(event)

        coVerify(exactly = 1) { recommendations.refreshProfile(7) }
    }
}
