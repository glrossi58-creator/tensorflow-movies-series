package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.repository.RatingRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class RatingProfileService(private val ratings: RatingRepository, private val data: RecommendationDataService, private val vector: PgVectorService, private val models: ModelLifecycleService) {
    suspend fun refresh(userId: Long) {
        vector.refreshUser(userId, ratings.findAllByUserId(userId).toList(), data.allSignals())
        models.status(userId)
    }
}
