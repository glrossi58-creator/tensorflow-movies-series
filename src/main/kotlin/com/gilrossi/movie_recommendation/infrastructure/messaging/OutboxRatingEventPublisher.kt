package com.gilrossi.movie_recommendation.infrastructure.messaging

import com.gilrossi.movie_recommendation.application.port.out.RatingEventPublisher
import com.gilrossi.movie_recommendation.event.RatingEvent
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Primary
@Component
class OutboxRatingEventPublisher(
    private val db: DatabaseClient, private val json: ObjectMapper,
    private val kafka: ObjectProvider<KafkaRatingEventPublisher>,
    @Value("\${app.kafka.enabled:false}") private val enabled: Boolean
) : RatingEventPublisher {
    private val logger = LoggerFactory.getLogger(javaClass)
    override suspend fun publish(event: RatingEvent) {
        if (!enabled) return
        db.sql("INSERT INTO rating_event_outbox(event_id,rating_id,payload,occurred_at) VALUES (:id,:rating,:payload,:time)")
            .bind("id", event.eventId).bind("rating", event.ratingId).bind("payload", json.writeValueAsString(event))
            .bind("time", event.occurredAt).fetch().rowsUpdated().awaitSingle()
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    suspend fun dispatch() {
        if (!enabled) return
        val publisher = kafka.ifAvailable ?: return
        val events = db.sql("SELECT event_id,payload FROM rating_event_outbox WHERE published_at IS NULL ORDER BY occurred_at LIMIT 50")
            .map { row, _ -> row.get("event_id", UUID::class.java)!! to row.get("payload", String::class.java)!! }
            .all().collectList().awaitSingle()
        for ((id, payload) in events) {
            try {
                publisher.publish(json.readValue(payload, RatingEvent::class.java))
                db.sql("UPDATE rating_event_outbox SET published_at=now() WHERE event_id=:id").bind("id", id).fetch().rowsUpdated().awaitSingle()
            } catch (e: Exception) {
                logger.warn("Kafka indisponível; evento {} permanece na outbox para nova tentativa", id)
                break
            }
        }
    }
}
