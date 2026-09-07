package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.model.Genre
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GenreRepository : CoroutineCrudRepository<Genre, Long>{

    suspend fun findByTmdbId(tmdbId: Int): Genre?

}

