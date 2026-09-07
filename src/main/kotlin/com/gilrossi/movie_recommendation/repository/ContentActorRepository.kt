package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.model.ContentActor
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import kotlinx.coroutines.flow.Flow

interface ContentActorRepository : CoroutineCrudRepository<ContentActor, Long> {

    suspend fun findByContentIdAndPersonId(
        contentId: Long,
        personId: Long
    ): ContentActor?

    fun findAllByContentId(contentId: Long): Flow<ContentActor>
}
