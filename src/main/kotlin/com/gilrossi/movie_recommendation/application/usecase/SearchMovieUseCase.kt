package com.gilrossi.movie_recommendation.application.usecase

import com.gilrossi.movie_recommendation.application.port.out.TmdbClient
import com.gilrossi.movie_recommendation.dto.response.TmdbCreditsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbMovieDetailsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSearchResponse
import org.springframework.stereotype.Service

@Service
class SearchMovieUseCase(
    private val tmdbClient: TmdbClient
) {

    suspend fun execute(title: String): TmdbSearchResponse {
        return tmdbClient.searchMovie(title)
    }

    suspend fun getDetails(tmdbId: Int): TmdbMovieDetailsResponse =
        tmdbClient.getMovieDetails(tmdbId)

    suspend fun getCredits(tmdbId: Int): TmdbCreditsResponse =
        tmdbClient.getMovieCredits(tmdbId)
}