package com.gilrossi.movie_recommendation.infrastructure.messaging

import com.gilrossi.movie_recommendation.application.port.out.RatingEventPublisher
import com.gilrossi.movie_recommendation.event.RatingEvent
import kotlinx.coroutines.future.await
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.kafka.enabled"], havingValue = "true")
class KafkaRatingEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : RatingEventPublisher {
    override suspend fun publish(event: RatingEvent) {
        kafkaTemplate.send(TOPIC, event.userId.toString(), event).await()
    }

    companion object { const val TOPIC = "rating.created" }
}
