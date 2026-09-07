package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.model.Content
import com.gilrossi.movie_recommendation.model.ContentType
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ContentRepository : CoroutineCrudRepository<Content, Long> {

    suspend fun findByTmdbIdAndType(
        tmdbId: Int,
        type: ContentType
    ): Content?
}