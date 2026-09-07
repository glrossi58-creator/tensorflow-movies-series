package com.gilrossi.movie_recommendation.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.gilrossi.movie_recommendation.dto.TmdbGenre

data class TmdbMovieDetailsResponse(
    val id: Int,
    val title: String,
    val overview: String?,
    val genres: List<TmdbGenre>,
    @JsonProperty("release_date")
    val releaseDate: String?,
    @JsonProperty("poster_path")
    val posterPath: String?,
    @JsonProperty("vote_average")
    val voteAverage: Double
)
