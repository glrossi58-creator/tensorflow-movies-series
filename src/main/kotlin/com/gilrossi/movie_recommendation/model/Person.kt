package com.gilrossi.movie_recommendation.model

import org.springframework.data.annotation.Id

data class Person(
    @Id
    val id: Long?,
    val tmdbId: Int,
    val name: String
)