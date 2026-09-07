package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.model.Person
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PersonRepository : CoroutineCrudRepository<Person, Long> {

    suspend fun findByTmdbId(tmdbId: Int): Person?
}