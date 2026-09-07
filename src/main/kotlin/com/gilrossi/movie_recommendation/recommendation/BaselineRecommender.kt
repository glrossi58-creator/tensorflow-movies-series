package com.gilrossi.movie_recommendation.recommendation

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("recommendation.weights")
class BaselineWeights {
    var genre: Double = 0.30
    var actor: Double = 0.20
    var director: Double = 0.20
    var movie: Double = 0.15
    var series: Double = 0.15

    fun asArray() = doubleArrayOf(genre, actor, director, movie, series)
}

@Component
class BaselineRecommender(private val weights: BaselineWeights) {
    fun score(features: FeatureVector): Double {
        val configured = weights.asArray()
        require(configured.all { it >= 0.0 } && configured.sum() > 0.0) { "Pesos do baseline são inválidos." }
        val weighted = configured.indices.sumOf { configured[it] * features.values[it] }
        return (weighted / configured.sum()).coerceIn(0.0, 1.0)
    }
}
