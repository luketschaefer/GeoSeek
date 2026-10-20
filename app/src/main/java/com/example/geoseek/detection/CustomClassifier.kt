package com.example.geoseek.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Our own image classifier for objects ML Kit's stock labeler has no label for (AirPods, for one).
 *
 * Loads `assets/custom_model.tflite` plus `assets/custom_labels.txt`, in the format Teachable Machine exports
 * (one class per line, optionally prefixed with its index: `0 AirPods`). Float models get pixels scaled to
 * -1..1, Teachable Machine's normalization; quantized uint8 models get raw pixels. Outputs are read as
 * per-class probabilities.
 *
 * Not thread-safe: use one instance from one thread.
 */
class CustomClassifier private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>,
) : AutoCloseable {

    private val inputShape = interpreter.getInputTensor(0).shape() // [1, height, width, 3]
    private val inputHeight = inputShape[1]
    private val inputWidth = inputShape[2]
    private val quantizedInput = interpreter.getInputTensor(0).dataType() == DataType.UINT8
    private val quantizedOutput = interpreter.getOutputTensor(0).dataType() == DataType.UINT8
    private val classCount = interpreter.getOutputTensor(0).shape().last()

    private val input = ByteBuffer
        .allocateDirect(inputWidth * inputHeight * 3 * if (quantizedInput) 1 else 4)
        .order(ByteOrder.nativeOrder())
    private val pixels = IntArray(inputWidth * inputHeight)

    /** Classifies an upright [image], returning every class with its probability. */
    fun classify(image: Bitmap): List<RawLabel> {
        val scaled = Bitmap.createScaledBitmap(image, inputWidth, inputHeight, true)
        scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        input.rewind()
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            if (quantizedInput) {
                input.put(r.toByte()).put(g.toByte()).put(b.toByte())
            } else {
                input.putFloat(r / 127.5f - 1f).putFloat(g / 127.5f - 1f).putFloat(b / 127.5f - 1f)
            }
        }

        val scores: List<Float> = if (quantizedOutput) {
            val out = Array(1) { ByteArray(classCount) }
            interpreter.run(input, out)
            out[0].map { (it.toInt() and 0xFF) / 255f }
        } else {
            val out = Array(1) { FloatArray(classCount) }
            interpreter.run(input, out)
            out[0].toList()
        }
        return scores.mapIndexedNotNull { i, score -> labels.getOrNull(i)?.let { RawLabel(it, score) } }
    }

    override fun close() = interpreter.close()

    companion object {
        private const val TAG = "CustomClassifier"
        const val MODEL_ASSET = "custom_model.tflite"
        const val LABELS_ASSET = "custom_labels.txt"

        /** True if the app ships a custom model and its labels, without loading either. */
        fun isBundled(context: Context): Boolean =
            context.assets.list("")?.let { MODEL_ASSET in it && LABELS_ASSET in it } ?: false

        /** Loads the bundled model, or returns null if the app doesn't ship one (or it can't be read). */
        fun load(context: Context): CustomClassifier? = try {
            val model = context.assets.open(MODEL_ASSET).use { stream ->
                val bytes = stream.readBytes()
                ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).also { it.rewind() }
            }
            val labels = context.assets.open(LABELS_ASSET).use { parseLabels(it.bufferedReader().readText()) }
            CustomClassifier(Interpreter(model, Interpreter.Options().setNumThreads(2)), labels)
        } catch (_: FileNotFoundException) {
            null
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't load the custom model; only the stock labeler will run.", e)
            null
        }

        /** Reads a labels file, dropping blank lines and Teachable Machine's leading class indices. */
        fun parseLabels(text: String): List<String> =
            text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { it.replaceFirst(Regex("^\\d+\\s+"), "") }
    }
}
