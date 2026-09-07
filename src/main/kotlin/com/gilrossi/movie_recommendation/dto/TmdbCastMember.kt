package com.gilrossi.movie_recommendation.dto

data class TmdbCastMember(
    val id: Int,
    val name: String,
    val character: String?,
    val order: Int
)
