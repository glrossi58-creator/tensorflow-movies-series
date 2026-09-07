package com.gilrossi.movie_recommendation.infrastructure.messaging

import com.gilrossi.movie_recommendation.event.RatingEvent
import com.gilrossi.movie_recommendation.application.usecase.RecommendationUseCase
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.kafka.enabled"], havingValue = "true")
class RatingEventConsumer(
    private val recommendationUseCase: RecommendationUseCase
) {
    @KafkaListener(topics = [KafkaRatingEventPublisher.TOPIC])
    suspend fun consume(event: RatingEvent) = recommendationUseCase.refreshProfile(event.userId)
}
