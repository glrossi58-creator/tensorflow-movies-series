package com.gilrossi.movie_recommendation.controller

import com.gilrossi.movie_recommendation.exception.GlobalExceptionHandler
import com.gilrossi.movie_recommendation.model.AppUser
import com.gilrossi.movie_recommendation.service.UserService
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant

class UserControllerTest {
    private val service = mockk<UserService>()
    private lateinit var client: WebTestClient
    private val user = AppUser(1, "Gil", "gil@example.com", Instant.EPOCH, Instant.EPOCH)

    @BeforeEach fun setup() {
        client = WebTestClient.bindToController(UserController(service)).controllerAdvice(GlobalExceptionHandler()).build()
    }

    @Test
    fun `creates lists updates and deletes user`() {
        coEvery { service.create(any()) } returns user
        coEvery { service.list() } returns listOf(user)
        coEvery { service.update(1, any()) } returns user.copy(name = "Gil Updated")
        coEvery { service.delete(1) } returns Unit

        client.post().uri("/users").contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name":"Gil","email":"gil@example.com"}""").exchange().expectStatus().isCreated
        client.get().uri("/users").exchange().expectStatus().isOk.expectBody().jsonPath("$[0].email").isEqualTo("gil@example.com")
        client.put().uri("/users/1").contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name":"Gil Updated","email":"gil@example.com"}""").exchange().expectStatus().isOk
        client.delete().uri("/users/1").exchange().expectStatus().isNoContent
    }

    @Test
    fun `validates email`() {
        client.post().uri("/users").contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name":"Gil","email":"invalid"}""").exchange().expectStatus().isBadRequest
    }
}
