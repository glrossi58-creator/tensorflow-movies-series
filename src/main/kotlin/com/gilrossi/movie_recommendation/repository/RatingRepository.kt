package com.gilrossi.movie_recommendation.repository

import com.gilrossi.movie_recommendation.model.Rating
import com.gilrossi.movie_recommendation.model.RatingTargetType
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface RatingRepository : CoroutineCrudRepository<Rating, Long> {
    fun findAllByUserId(userId: Long): Flow<Rating>
    fun findAllByUserIdIn(userIds: Collection<Long>): Flow<Rating>
    fun findAllByContentIdIsNotNull(): Flow<Rating>
    suspend fun findByUserIdAndTargetTypeAndContentId(userId: Long, targetType: RatingTargetType, contentId: Long): Rating?
    suspend fun findByUserIdAndTargetTypeAndPersonId(userId: Long, targetType: RatingTargetType, personId: Long): Rating?
    suspend fun findByUserIdAndTargetTypeAndGenreId(userId: Long, targetType: RatingTargetType, genreId: Long): Rating?
}
