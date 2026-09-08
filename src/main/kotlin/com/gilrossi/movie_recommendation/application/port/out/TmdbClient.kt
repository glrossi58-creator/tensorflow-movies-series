package com.gilrossi.movie_recommendation.application.port.out

import com.gilrossi.movie_recommendation.dto.response.TmdbCreditsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbMovieDetailsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSearchResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSeriesAggregateCreditsResponse
import com.gilrossi.movie_recommendation.dto.response.TmdbSeriesDetailsResponse

interface TmdbClient {
    suspend fun searchUniversal(query: String, type: com.gilrossi.movie_recommendation.discovery.DiscoveryType?): com.gilrossi.movie_recommendation.discovery.TmdbDiscoveryResponse
    suspend fun popular(type: com.gilrossi.movie_recommendation.discovery.DiscoveryType, page: Int): com.gilrossi.movie_recommendation.discovery.TmdbDiscoveryResponse
    suspend fun getPersonDetails(tmdbId: Int): com.gilrossi.movie_recommendation.discovery.TmdbPersonDetails
    suspend fun getPersonCredits(tmdbId: Int): com.gilrossi.movie_recommendation.discovery.TmdbPersonCredits

    suspend fun searchMovie(title: String): TmdbSearchResponse

    suspend fun getMovieDetails(tmdbId: Int): TmdbMovieDetailsResponse

    suspend fun getMovieCredits(tmdbId: Int): TmdbCreditsResponse

    suspend fun getSeriesDetails(tmdbId: Int): TmdbSeriesDetailsResponse

    suspend fun getSeriesAggregateCredits(tmdbId: Int): TmdbSeriesAggregateCreditsResponse
}
