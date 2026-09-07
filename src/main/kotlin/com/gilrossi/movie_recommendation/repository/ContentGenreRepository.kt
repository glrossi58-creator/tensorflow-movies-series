package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.model.ContentGenre
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import kotlinx.coroutines.flow.Flow

interface ContentGenreRepository : CoroutineCrudRepository<ContentGenre, Long> {

    suspend fun findByContentIdAndGenreId(
        contentId: Long,
        genreId: Long
    ): ContentGenre?

    fun findAllByContentId(contentId: Long): Flow<ContentGenre>
}
