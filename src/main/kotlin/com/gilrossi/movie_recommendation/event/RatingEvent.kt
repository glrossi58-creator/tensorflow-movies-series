package com.gilrossi.movie_recommendation.event

import com.gilrossi.movie_recommendation.model.RatingTargetType
import java.time.Instant
import java.util.UUID

data class RatingEvent(
    val eventId: UUID,
    val ratingId: Long,
    val userId: Long,
    val targetType: RatingTargetType,
    val targetId: Long,
    val value: Int,
    val occurredAt: Instant
)
