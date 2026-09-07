package com.gilrossi.movie_recommendation.model

import org.springframework.data.annotation.Id

data class ContentGenre(
    @Id
    val id: Long?,
    val contentId: Long,
    val genreId: Long
)