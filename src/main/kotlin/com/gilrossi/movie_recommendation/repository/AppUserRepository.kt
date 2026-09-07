package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.model.AppUser
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AppUserRepository : CoroutineCrudRepository<AppUser, Long> {
    suspend fun findByEmailIgnoreCase(email: String): AppUser?
}
