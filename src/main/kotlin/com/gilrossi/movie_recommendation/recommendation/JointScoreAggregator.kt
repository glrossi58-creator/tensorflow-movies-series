package com.gilrossi.movie_recommendation.recommendation

import org.springframework.stereotype.Component

@Component
class JointScoreAggregator {
    fun aggregate(scores: Collection<Double>): Double {
        require(scores.size >= 2) { "São necessários ao menos dois scores." }
        require(scores.all { it in 0.0..1.0 }) { "Scores devem estar entre 0 e 1." }
        val harmonicMean = scores.size / scores.sumOf { 1.0 / it.coerceAtLeast(MINIMUM_DENOMINATOR) }
        return (harmonicMean * HARMONIC_WEIGHT + requireNotNull(scores.minOrNull()) * MINIMUM_WEIGHT)
            .coerceIn(0.0, 1.0)
    }

    companion object {
        private const val MINIMUM_DENOMINATOR = 0.01
        private const val HARMONIC_WEIGHT = 0.7
        private const val MINIMUM_WEIGHT = 0.3
    }
}
