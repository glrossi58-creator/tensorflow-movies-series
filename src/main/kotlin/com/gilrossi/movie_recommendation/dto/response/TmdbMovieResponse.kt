package com.gilrossi.movie_recommendation.dto.response

data class TmdbMovieResponse(
    val id: Long,
    val title: String,
    val overview: String?
)
