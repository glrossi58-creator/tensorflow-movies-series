package com.gilrossi.movie_recommendation.controller

import com.gilrossi.movie_recommendation.application.usecase.ImportMovieUseCase
import com.gilrossi.movie_recommendation.application.usecase.ImportSeriesUseCase
import com.gilrossi.movie_recommendation.application.usecase.SearchMovieUseCase
import com.gilrossi.movie_recommendation.dto.request.ContentRequest
import com.gilrossi.movie_recommendation.dto.response.TmdbCreditsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbMovieDetailsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSearchResponse
import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.service.ContentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
class ContentController (

    private val contentsService: ContentService,
    private val searchMovieUseCase: SearchMovieUseCase,
    private val importMovieUseCase: ImportMovieUseCase,
    private val importSeriesUseCase: ImportSeriesUseCase

){

    @GetMapping("/contents")
    suspend fun getContent(): List<Content>{

        return contentsService.getContents()

    }

    @PostMapping("/contents")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createContent(
        @RequestBody @Valid request: ContentRequest
    ): Content{

        return contentsService.createContent(request)

    }

    @GetMapping("/contents/{id}")
    suspend fun getContentById(
        @PathVariable id: Long
    ): Content {
        return contentsService.getContentById(id)
    }

    @PutMapping("/contents/{id}")
    suspend fun updateContent (
        @PathVariable id: Long,
        @RequestBody @Valid request: ContentRequest
    ) : Content {
        return contentsService.updateContent(id, request)
    }

    @DeleteMapping("/contents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteContent (@PathVariable id: Long){
        contentsService.deleteContent(id)
    }

    @GetMapping("/contents/search")
    suspend fun searchMovie(
        @RequestParam title: String
    ): TmdbSearchResponse {
        return searchMovieUseCase.execute(title)
    }

    @GetMapping("/contents/tmdb/{tmdbId}/details")
    suspend fun getDetails(@PathVariable tmdbId: Int): TmdbMovieDetailsResponse =
        searchMovieUseCase.getDetails(tmdbId)

    @GetMapping("/contents/tmdb/{tmdbId}/credits")
    suspend fun getCredits(@PathVariable tmdbId: Int): TmdbCreditsResponse =
        searchMovieUseCase.getCredits(tmdbId)

    @PostMapping("/contents/tmdb/{tmdbId}/import")
    suspend fun importMovie(
        @PathVariable tmdbId: Int
    ): Content {

        return importMovieUseCase.execute(tmdbId)
    }

    @PostMapping("/contents/tmdb/series/{tmdbId}/import")
    suspend fun importSeries(@PathVariable tmdbId: Int): Content =
        importSeriesUseCase.execute(tmdbId)
}
