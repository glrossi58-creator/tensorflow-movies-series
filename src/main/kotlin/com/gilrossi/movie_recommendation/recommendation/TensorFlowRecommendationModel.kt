package com.gilrossi.movie_recommendation.recommendation

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.tensorflow.Graph
import org.tensorflow.Session
import org.tensorflow.framework.optimizers.GradientDescent
import org.tensorflow.ndarray.Shape
import org.tensorflow.op.Ops
import org.tensorflow.op.core.Placeholder
import org.tensorflow.types.TFloat32
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.exp
import kotlin.random.Random

data class TrainedRecommendationModel(
    val weights: FloatArray,
    val bias: Float,
    val trainingLoss: Double,
    val validationLoss: Double,
    val trainingSamples: Int,
    val validationSamples: Int
)

@Component
class TensorFlowRecommendationModel(
    @Value("\${recommendation.ml.epochs:150}") private val epochs: Int,
    @Value("\${recommendation.ml.learning-rate:0.08}") private val learningRate: Float,
    @Value("\${recommendation.ml.seed:42}") private val seed: Int,
    @Value("\${recommendation.ml.model-directory:./data/models}") modelDirectory: String
) {
    private val directory = Path.of(modelDirectory)

    fun train(examples: List<LabeledExample>, validationExamples: List<LabeledExample>? = null): TrainedRecommendationModel {
        require(examples.isNotEmpty()) { "Dataset vazio." }
        val shuffled = examples.shuffled(Random(seed))
        val validationSize = if (examples.size > 4) maxOf(1, examples.size / 5) else 0
        val validation = validationExamples ?: shuffled.take(validationSize)
        val training = if (validationExamples != null) shuffled else shuffled.drop(validationSize).ifEmpty { shuffled }

        Graph().use { graph ->
            val tf = Ops.create(graph)
            val x = tf.placeholder(TFloat32::class.java, Placeholder.shape(Shape.of(-1, FeatureVector.FEATURE_COUNT.toLong())))
            val y = tf.placeholder(TFloat32::class.java, Placeholder.shape(Shape.of(-1, 1)))
            val random = Random(seed)
            val initialWeights = Array(FeatureVector.FEATURE_COUNT) { floatArrayOf((random.nextFloat() - 0.5f) * 0.1f) }
            val weights = tf.variable(tf.constant(initialWeights))
            val bias = tf.variable(tf.constant(floatArrayOf(0f)))
            val prediction = tf.math.sigmoid(tf.math.add(tf.linalg.matMul(x, weights), bias))
            val loss = tf.math.mean(tf.math.square(tf.math.sub(prediction, y)), tf.constant(intArrayOf(0, 1)))
            val trainOperation = GradientDescent(graph, "recommendation-gradient-descent", learningRate).minimize(loss)

            Session(graph).use { session ->
                session.initialize()
                toTensor(training.map(LabeledExample::features)).use { xTensor ->
                    toLabelTensor(training.map(LabeledExample::label)).use { yTensor ->
                        repeat(epochs) {
                            session.runner().addTarget(trainOperation).feed(x, xTensor).feed(y, yTensor).run().use { }
                        }
                        session.runner().feed(x, xTensor).feed(y, yTensor)
                            .fetch(weights).fetch(bias).fetch(loss).run().use { result ->
                                val learnedWeights = result.get(0) as TFloat32
                                val learnedBias = result.get(1) as TFloat32
                                val trainingLoss = (result.get(2) as TFloat32).getFloat().toDouble()
                                val values = FloatArray(FeatureVector.FEATURE_COUNT) { learnedWeights.getFloat(it.toLong(), 0) }
                                val biasValue = learnedBias.getFloat(0)
                                val validationLoss = if (validation.isEmpty()) trainingLoss else mse(validation, values, biasValue)
                                return TrainedRecommendationModel(
                                    values, biasValue, trainingLoss, validationLoss, training.size, validation.size
                                )
                            }
                    }
                }
            }
        }
    }

    fun predict(features: FloatArray, model: TrainedRecommendationModel): Double {
        require(features.size == FeatureVector.FEATURE_COUNT)
        val logit = features.indices.sumOf { features[it].toDouble() * model.weights[it] } + model.bias
        return (1.0 / (1.0 + exp(-logit))).coerceIn(0.0, 1.0)
    }

    fun save(userId: Long, model: TrainedRecommendationModel) {
        saveTo(path(userId), model)
    }

    fun path(userId: Long): Path = directory.resolve("user-$userId.properties")
    fun versionPath(userId: Long, version: Int): Path = directory.resolve("user-$userId-v$version.properties")

    fun saveTo(file: Path, model: TrainedRecommendationModel) {
        Files.createDirectories(directory)
        val content = buildString {
            appendLine("weights=${model.weights.joinToString(",")}")
            appendLine("bias=${model.bias}")
            appendLine("trainingLoss=${model.trainingLoss}")
            appendLine("validationLoss=${model.validationLoss}")
            appendLine("trainingSamples=${model.trainingSamples}")
            appendLine("validationSamples=${model.validationSamples}")
        }
        val temporary = Files.createTempFile(directory, "weights-", ".tmp")
        Files.writeString(temporary, content)
        try {
            Files.move(temporary, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun load(userId: Long): TrainedRecommendationModel? {
        return loadFrom(path(userId))
    }

    fun loadFrom(file: Path): TrainedRecommendationModel? {
        if (!Files.exists(file)) return null
        val values = Files.readAllLines(file).associate { line -> line.substringBefore('=') to line.substringAfter('=') }
        val weights = values.getValue("weights").split(',').map(String::toFloat).toFloatArray()
        if (weights.size != FeatureVector.FEATURE_COUNT) return null
        return TrainedRecommendationModel(
            weights, values.getValue("bias").toFloat(), values.getValue("trainingLoss").toDouble(),
            values.getValue("validationLoss").toDouble(), values.getValue("trainingSamples").toInt(),
            values.getValue("validationSamples").toInt()
        )
    }

    private fun toTensor(rows: List<FloatArray>): TFloat32 = TFloat32.tensorOf(
        Shape.of(rows.size.toLong(), FeatureVector.FEATURE_COUNT.toLong())
    ) { tensor -> rows.forEachIndexed { i, row -> row.forEachIndexed { j, value -> tensor.setFloat(value, i.toLong(), j.toLong()) } } }

    private fun toLabelTensor(labels: List<Float>): TFloat32 = TFloat32.tensorOf(Shape.of(labels.size.toLong(), 1)) {
        tensor -> labels.forEachIndexed { index, value -> tensor.setFloat(value, index.toLong(), 0) }
    }

    private fun mse(examples: List<LabeledExample>, weights: FloatArray, bias: Float): Double = examples
        .map { example ->
            val prediction = 1.0 / (1.0 + exp(-(example.features.indices.sumOf { example.features[it].toDouble() * weights[it] } + bias)))
            (prediction - example.label) * (prediction - example.label)
        }.average()
}
