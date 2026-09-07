package com.gilrossi.movie_recommendation.recommendation

import com.gilrossi.movie_recommendation.model.ContentType
import org.springframework.stereotype.Component
import kotlin.math.sqrt

@Component
class ContentEmbeddingGenerator {
    fun generate(signals: ContentSignals): FloatArray {
        val vector = FloatArray(DIMENSIONS)
        vector[0] = if (signals.content.type == ContentType.MOVIE) 1f else -1f
        addIds(vector, signals.genreIds, 1, 3, 1.0f)
        addIds(vector, signals.actorIds, 3, 3, 0.7f)
        addIds(vector, signals.directorOrCreatorIds, 6, 2, 0.9f)
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0f) vector.indices.forEach { vector[it] /= norm }
        return vector
    }

    private fun addIds(vector: FloatArray, ids: Set<Long>, offset: Int, width: Int, weight: Float) {
        ids.forEach { id ->
            val bucket = offset + Math.floorMod(id.hashCode(), width)
            val sign = if ((id xor (id ushr 16)) and 1L == 0L) 1f else -1f
            vector[bucket] += sign * weight
        }
    }

    companion object { const val DIMENSIONS = 8 }
}
