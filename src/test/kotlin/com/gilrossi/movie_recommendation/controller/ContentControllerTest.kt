package com.gilrossi.movie_recommendation.controller

import com.gilrossi.movie_recommendation.application.usecase.ImportMovieUseCase
import com.gilrossi.movie_recommendation.application.usecase.ImportSeriesUseCase
import com.gilrossi.movie_recommendation.application.usecase.SearchMovieUseCase
import com.gilrossi.movie_recommendation.exception.ContentNotFoundException
import com.gilrossi.movie_recommendation.exception.GlobalExceptionHandler
import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.ContentType
import com.gilrossi.movie_recommendation.service.ContentService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.http.MediaType

class ContentControllerTest {
    private val service = mockk<ContentService>()
    private val search = mockk<SearchMovieUseCase>()
    private val importMovie = mockk<ImportMovieUseCase>()
    private val importSeries = mockk<ImportSeriesUseCase>()
    private lateinit var client: WebTestClient

    @BeforeEach
    fun setup() {
        client = WebTestClient.bindToController(ContentController(service, search, importMovie, importSeries))
            .controllerAdvice(GlobalExceptionHandler()).build()
    }

    @Test
    fun `creates content with 201`() {
        coEvery { service.createContent(any()) } returns content(1)
        client.post().uri("/contents").contentType(MediaType.APPLICATION_JSON).bodyValue("""{"title":"Matrix","type":"MOVIE"}""")
            .exchange().expectStatus().isCreated.expectBody().jsonPath("$.id").isEqualTo(1)
    }

    @Test
    fun `rejects blank title and invalid enum`() {
        client.post().uri("/contents").contentType(MediaType.APPLICATION_JSON).bodyValue("""{"title":"","type":"MOVIE"}""")
            .exchange().expectStatus().isBadRequest
        client.post().uri("/contents").contentType(MediaType.APPLICATION_JSON).bodyValue("""{"title":"X","type":"VIDEO"}""")
            .exchange().expectStatus().isBadRequest
    }

    @Test
    fun `returns 404 and deletes with 204`() {
        coEvery { service.getContentById(99) } throws ContentNotFoundException("Conteúdo não encontrado.")
        client.get().uri("/contents/99").exchange().expectStatus().isNotFound
        coEvery { service.deleteContent(1) } returns Unit
        client.delete().uri("/contents/1").exchange().expectStatus().isNoContent
    }

    @Test
    fun `lists and updates content`() {
        coEvery { service.getContents() } returns listOf(content(1))
        coEvery { service.updateContent(1, any()) } returns content(1).copy(title = "Updated")
        client.get().uri("/contents").exchange().expectStatus().isOk.expectBody().jsonPath("$[0].id").isEqualTo(1)
        client.put().uri("/contents/1").contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"title":"Updated","type":"MOVIE"}""").exchange()
            .expectStatus().isOk.expectBody().jsonPath("$.title").isEqualTo("Updated")
    }

    @Test
    fun `exposes both import endpoints`() {
        coEvery { importMovie.execute(603) } returns content(8)
        coEvery { importSeries.execute(1399) } returns content(9, ContentType.SERIES)
        client.post().uri("/contents/tmdb/603/import").exchange().expectStatus().isOk
        client.post().uri("/contents/tmdb/series/1399/import").exchange().expectStatus().isOk
        coVerify { importMovie.execute(603); importSeries.execute(1399) }
    }

    private fun content(id: Long, type: ContentType = ContentType.MOVIE) =
        Content(id, "Matrix", 603, type, null, null, null)
}
