package com.gilrossi.movie_recommendation.config

import com.gilrossi.movie_recommendation.application.port.out.RatingEventPublisher
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RatingEventConfig {
    @Bean
    @ConditionalOnMissingBean(RatingEventPublisher::class)
    fun noOpRatingEventPublisher(): RatingEventPublisher = object : RatingEventPublisher {
        override suspend fun publish(event: com.gilrossi.movie_recommendation.event.RatingEvent) = Unit
    }
}
