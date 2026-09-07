package com.gilrossi.movie_recommendation.infrastructure.messaging

import com.gilrossi.movie_recommendation.event.RatingEvent
import com.gilrossi.movie_recommendation.model.RatingTargetType
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JacksonJsonSerializer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Testcontainers(disabledWithoutDocker = true)
class KafkaRatingEventIntegrationTest {
    @Test
    fun `publisher sends rating created event to kafka`() = runBlocking {
        val producerProps = mapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonJsonSerializer::class.java
        )
        val factory = DefaultKafkaProducerFactory<String, Any>(producerProps)
        val template = KafkaTemplate(factory)
        val publisher = KafkaRatingEventPublisher(template)
        val event = RatingEvent(UUID.randomUUID(), 1, 2, RatingTargetType.MOVIE, 3, 5, Instant.now())
        val consumer = KafkaConsumer<String, String>(mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "rating-test-${UUID.randomUUID()}",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
        ))
        try {
            consumer.subscribe(listOf(KafkaRatingEventPublisher.TOPIC))
            consumer.poll(Duration.ofMillis(200))
            publisher.publish(event)
            val records = consumer.poll(Duration.ofSeconds(10))
            assertTrue(records.any { it.key() == "2" && it.value().contains(event.eventId.toString()) })
        } finally {
            consumer.close()
            template.destroy()
            factory.destroy()
        }
    }

    companion object {
        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.0"))
    }
}
