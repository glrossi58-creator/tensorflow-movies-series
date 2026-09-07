package com.gilrossi.movie_recommendation.recommendation

import org.springframework.stereotype.Component

@Component
class RatingNormalizer(
    private val minValue: Int = MIN_RATING,
    private val maxValue: Int = MAX_RATING
) {
    init {
        require(maxValue > minValue) { "maxValue deve ser maior que minValue." }
    }

    fun normalize(value: Int): Double {
        require(value in minValue..maxValue) { "Rating deve estar entre $minValue e $maxValue." }
        return (value - minValue).toDouble() / (maxValue - minValue)
    }

    companion object {
        const val MIN_RATING = 1
        const val MAX_RATING = 5
    }
}
