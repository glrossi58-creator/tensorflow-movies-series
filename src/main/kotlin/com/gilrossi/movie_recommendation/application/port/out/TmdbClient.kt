package com.gilrossi.movie_recommendation.application.port.out

import com.gilrossi.movie_recommendation.dto.response.TmdbCreditsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbMovieDetailsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSearchResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSeriesAggregateCreditsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSeriesDetailsResponse

interface TmdbClient {

    suspend fun searchMovie(title: String): TmdbSearchResponse

    suspend fun getMovieDetails(tmdbId: Int): TmdbMovieDetailsResponse

    suspend fun getMovieCredits(tmdbId: Int): TmdbCreditsResponse

    suspend fun getSeriesDetails(tmdbId: Int): TmdbSeriesDetailsResponse

    suspend fun getSeriesAggregateCredits(tmdbId: Int): TmdbSeriesAggregateCreditsResponse
}
