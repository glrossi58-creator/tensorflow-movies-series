package com.gilrossi.movie_recommendation.model

import org.springframework.data.annotation.Id

data class ContentActor(
    @Id
    val id: Long?,
    val contentId: Long,
    val personId: Long
)