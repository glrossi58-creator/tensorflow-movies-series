package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.model.ContentDirector
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import kotlinx.coroutines.flow.Flow

interface ContentDirectorRepository : CoroutineCrudRepository<ContentDirector, Long> {

    suspend fun findByContentIdAndPersonId(
        contentId: Long,
        personId: Long
    ): ContentDirector?

    fun findAllByContentId(contentId: Long): Flow<ContentDirector>
}
