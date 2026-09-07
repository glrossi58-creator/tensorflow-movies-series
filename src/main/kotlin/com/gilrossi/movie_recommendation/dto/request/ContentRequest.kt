package com.gilrossi.movie_recommendation.dto.request

import com.gilrossi.movie_recommendation.model.ContentType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

data class ContentRequest(

    @field:NotBlank(message = "O título é obrigatório.")
    val title: String,

    val tmdbId: Int? = null,

    val type: ContentType,

    val overview: String? = null,

    val releaseDate: LocalDate? = null,

    val posterPath: String? = null
)