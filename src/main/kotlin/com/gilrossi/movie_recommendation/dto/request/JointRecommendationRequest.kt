package com.gilrossi.movie_recommendation.dto.request

import jakarta.validation.constraints.Size

data class JointRecommendationRequest(
    @field:Size(min = 2, max = 20, message = "Informe entre 2 e 20 usuários.")
    val userIds: List<Long>,
    val limit: Int = 20
)
