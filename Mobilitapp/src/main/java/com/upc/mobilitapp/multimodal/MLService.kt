package com.upc.mobilitapp.multimodal

import android.content.Context
import android.content.res.AssetFileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

/**
 * MLService is a class that provides machine learning functionality for multimodal transportation analysis.
 *
 * @property ctx The context used to access the application's assets.
 * @property model The TensorFlow Lite model interpreter used for inference.
 * @property NAMES A map that maps class labels to their corresponding names.
 * @property MODEL_FILE_NAME The name of the TensorFlow Lite model file.
 * @property NUM_STEPS The number of steps in the input data.
 * @property NUM_FEATURES The number of features in each step of the input data.
 * @property MEAN_TEST The mean values used for standardizing the input data.
 * @property STD_TEST The standard deviation values used for standardizing the input data.
 */
class MLService(val ctx: Context) {
    private lateinit var model: Interpreter

    private val NAMES = mapOf(
        0 to "Bicycle",
        1 to "Bus",
        2 to "Car",
        3 to "Moto",
        4 to "Run",
        5 to "STILL",
        6 to "Metro",
        7 to "Train",
        8 to "Tram",
        9 to "WALK",
        10 to "E-Scooter"
    )

    private val MODEL_FILE_NAME = "2feb-notf.tflite"

    private val NUM_STEPS = 512
    private val NUM_FEATURES = 9

    /**
     * Initialize service by loading tflite interpreter.
     */
    fun initialize() {
        model = Interpreter(loadModelFile()!!, null)
    }

    /**
     * Load model in MappedByteBuffer format.
     */
    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer? {
        val assetFileDescriptor: AssetFileDescriptor = ctx.assets.openFd(MODEL_FILE_NAME)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val len = assetFileDescriptor.length

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, len)
    }

    /**
     * Model inference of a single sample.
     *
     * @param sample float array of original sample.
     * @return activity prediction.
     */
    fun singleInference(sample: Array<FloatArray>): String? {
        model.allocateTensors()
        val input = Array(1) {
            Array(NUM_STEPS) {
                FloatArray(NUM_FEATURES)
            }
        }
        for (i in 0 until NUM_STEPS){
            input[0][i] = sample[i] //standardizeData(sample[i])
        }
        val output =  Array(1) {
            FloatArray(NAMES.size)
        }

        model.run(input, output)
        var maxval = 0.0f
        var maxIdx = -1
        for (i in 0 until output[0].size) {
            if (maxval < output[0][i]) {
                maxval = output[0][i]
                maxIdx = i
            }
        }

        return NAMES[maxIdx].toString()
    }

    /**
     * Model overall prediction.
     *
     * @param matrix of a number of samples.
     * @return overall prediction.
     */
    fun overallPrediction(matrix: Array<Array<FloatArray>>): Pair<String, MutableMap<String, Int>> {
        var count = 0
        var predictions = mutableMapOf<String, Int>()
        NAMES.forEach { act ->
            predictions[act.value] = 0
        }
        matrix.forEach { sample ->
            val act = singleInference(sample).toString()
            predictions[act] = predictions[act]!! + 1
            ++count
        }
        val maxPred=predictions.maxWith(Comparator { x, y -> x.value.compareTo(y.value)})
        return maxPred.key to  predictions
    }
}