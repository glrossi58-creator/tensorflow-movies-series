package com.gilrossi.movie_recommendation.model

import org.springframework.data.annotation.Id
import java.time.LocalDate

data class Content(
    @Id
    val id: Long?,
    val title: String,
    val tmdbId: Int?,
    val type: ContentType,
    val overview: String?,
    val releaseDate: LocalDate?,
    val posterPath: String?
)