package com.gilrossi.movie_recommendation.dto.request

import com.gilrossi.movie_recommendation.model.RatingTargetType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive

data class RatingRequest(
    val targetType: RatingTargetType,
    @field:Positive(message = "targetId deve ser positivo.")
    val targetId: Long,
    @field:Min(1, message = "A nota mínima é 1.")
    @field:Max(5, message = "A nota máxima é 5.")
    val value: Int
)
