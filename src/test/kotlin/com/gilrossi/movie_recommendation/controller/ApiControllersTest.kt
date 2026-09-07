package com.gilrossi.movie_recommendation.controller

import com.gilrossi.movie_recommendation.application.usecase.RatingUseCase
import com.gilrossi.movie_recommendation.application.usecase.RecommendationUseCase
import com.gilrossi.movie_recommendation.dto.response.RatingResponse
import com.gilrossi.movie_recommendation.exception.GlobalExceptionHandler
import com.gilrossi.movie_recommendation.model.RatingTargetType
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.http.MediaType
import java.time.Instant

class ApiControllersTest {
    @Test
    fun `rating endpoint validates scale and returns normalized rating`() {
        val useCase = mockk<RatingUseCase>()
        coEvery { useCase.rate(1, any()) } returns RatingResponse(3, 1, RatingTargetType.GENRE, 7, 5, 1.0, Instant.EPOCH)
        val client = WebTestClient.bindToController(RatingController(useCase)).controllerAdvice(GlobalExceptionHandler()).build()

        client.post().uri("/users/1/ratings").contentType(MediaType.APPLICATION_JSON).bodyValue("""{"targetType":"GENRE","targetId":7,"value":5}""")
            .exchange().expectStatus().isCreated.expectBody().jsonPath("$.normalizedValue").isEqualTo(1.0)
        client.post().uri("/users/1/ratings").contentType(MediaType.APPLICATION_JSON).bodyValue("""{"targetType":"GENRE","targetId":7,"value":6}""")
            .exchange().expectStatus().isBadRequest
    }

    @Test
    fun `joint recommendation validates at least two users`() {
        val useCase = mockk<RecommendationUseCase>()
        val client = WebTestClient.bindToController(RecommendationController(useCase)).controllerAdvice(GlobalExceptionHandler()).build()
        client.post().uri("/recommendations/joint").contentType(MediaType.APPLICATION_JSON).bodyValue("""{"userIds":[1]}""")
            .exchange().expectStatus().isBadRequest
    }
}
