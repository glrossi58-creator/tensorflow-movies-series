package com.gilrossi.movie_recommendation.dto.response

import com.gilrossi.movie_recommendation.model.RatingTargetType
import java.time.Instant

data class RatingResponse(
    val id: Long,
    val userId: Long,
    val targetType: RatingTargetType,
    val targetId: Long,
    val value: Int,
    val normalizedValue: Double,
    val updatedAt: Instant
)
