package com.gilrossi.movie_recommendation.model

import org.springframework.data.annotation.Id
import java.time.Instant

data class Rating(
    @Id val id: Long?,
    val userId: Long,
    val targetType: RatingTargetType,
    val value: Int,
    val contentId: Long?,
    val personId: Long?,
    val genreId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun targetId(): Long = contentId ?: personId ?: genreId
        ?: error("Rating sem alvo.")
}
