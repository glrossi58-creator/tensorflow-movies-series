package com.gilrossi.movie_recommendation.controller

import com.gilrossi.movie_recommendation.application.usecase.RecommendationUseCase
import com.gilrossi.movie_recommendation.dto.request.JointRecommendationRequest
import com.gilrossi.movie_recommendation.recommendation.JointRecommendationResult
import com.gilrossi.movie_recommendation.recommendation.RecommendationResult
import com.gilrossi.movie_recommendation.recommendation.TrainedRecommendationModel
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/recommendations")
class RecommendationController(private val useCase: RecommendationUseCase) {
    @GetMapping("/users/{userId}")
    suspend fun individual(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<RecommendationResult> = useCase.recommend(userId, limit)

    @PostMapping("/users/{userId}/train")
    suspend fun train(@PathVariable userId: Long): TrainedRecommendationModel = useCase.train(userId)

    @PostMapping("/joint")
    suspend fun joint(@RequestBody @Valid request: JointRecommendationRequest): List<JointRecommendationResult> =
        useCase.recommendTogether(request.userIds, request.limit)
}
