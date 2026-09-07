package com.gilrossi.movie_recommendation.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.gilrossi.movie_recommendation.dto.TmdbCreator
import com.gilrossi.movie_recommendation.dto.TmdbGenre

data class TmdbSeriesDetailsResponse(
    val id: Int,
    val name: String,
    val overview: String?,
    val genres: List<TmdbGenre>,

    @JsonProperty("first_air_date")
    val firstAirDate: String?,

    @JsonProperty("poster_path")
    val posterPath: String?,

    @JsonProperty("created_by")
    val createdBy: List<TmdbCreator>
)