package com.gilrossi.movie_recommendation.dto.response

data class TmdbSearchResponse(
    val page: Int,
    val results: List<TmdbMovieResponse>,
    val total_pages: Int,
    val total_results: Int
)