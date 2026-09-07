package com.gilrossi.movie_recommendation.application.port.out

import com.gilrossi.movie_recommendation.event.RatingEvent

interface RatingEventPublisher {
    suspend fun publish(event: RatingEvent)
}
