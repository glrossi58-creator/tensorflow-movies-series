package com.gilrossi.movie_recommendation.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("app_user")
data class AppUser(
    @Id val id: Long?,
    val name: String,
    val email: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
