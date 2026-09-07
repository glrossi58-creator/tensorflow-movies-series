package com.gilrossi.movie_recommendation.recommendation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TensorFlowRecommendationModelTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `trains predicts persists and reloads a real tensorflow model`() {
        val modelWrapper = TensorFlowRecommendationModel(120, 0.1f, 42, directory.toString())
        val examples = (0 until 20).map { index ->
            val positive = index >= 10
            val value = if (positive) 1f else 0f
            LabeledExample(index.toLong(), floatArrayOf(value, value, value, value, value, value, value, value), value)
        }

        val model = modelWrapper.train(examples)
        val low = modelWrapper.predict(FloatArray(8), model)
        val high = modelWrapper.predict(FloatArray(8) { 1f }, model)
        modelWrapper.save(7, model)
        val loaded = modelWrapper.load(7)

        assertTrue(model.trainingLoss.isFinite())
        assertTrue(high > low)
        assertNotNull(loaded)
        assertEquals(high, modelWrapper.predict(FloatArray(8) { 1f }, requireNotNull(loaded)), 0.00001)
    }
}
