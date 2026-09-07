package com.gilrossi.movie_recommendation.controller

import com.gilrossi.movie_recommendation.application.usecase.RatingUseCase
import com.gilrossi.movie_recommendation.dto.request.RatingRequest
import com.gilrossi.movie_recommendation.dto.response.RatingResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users/{userId}/ratings")
class RatingController(private val useCase: RatingUseCase) {
    @GetMapping suspend fun list(@PathVariable userId: Long): List<RatingResponse> = useCase.list(userId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@PathVariable userId: Long, @RequestBody @Valid request: RatingRequest) = useCase.rate(userId, request)

    @PutMapping
    suspend fun upsert(@PathVariable userId: Long, @RequestBody @Valid request: RatingRequest) = useCase.rate(userId, request)

    @DeleteMapping("/{ratingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun delete(@PathVariable userId: Long, @PathVariable ratingId: Long) = useCase.delete(userId, ratingId)
}
