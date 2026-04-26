package com.aqi.weather.aqiPerdiction

import android.content.Context
import android.util.Log
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.InterpreterApi
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class AQIModel(private val context: Context) {
    private var interpreter: InterpreterApi? = null
    private val initializationDeferred = CompletableDeferred<Unit>()

    init {
        TfLite.initialize(context).addOnSuccessListener {
            try {
                val buffer = loadModelFile(context)
                val options = InterpreterApi.Options()
                    .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)

                interpreter = InterpreterApi.create(buffer, options)
                initializationDeferred.complete(Unit)
            } catch (e: Exception) {
                Log.e("AQIModel", "Error creating interpreter", e)
                initializationDeferred.completeExceptionally(e)
            }
        }.addOnFailureListener {
            initializationDeferred.completeExceptionally(it)
        }
    }

    private fun loadModelFile(context: Context): ByteBuffer {
        val fileDescriptor = context.assets.openFd("aqi_regression.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    suspend fun predict(input: FloatArray): Int {
        require(input.size == 14) { "Input must have exactly 14 features" }

        initializationDeferred.await()

        return withContext(Dispatchers.Default) {
            // Input: [1, 14] - 14 features
            val inputArray = arrayOf(input)

            // Output: [1, 1] - single AQI value
            val output = Array(1) { FloatArray(1) }

            interpreter?.run(inputArray, output)

            // Return predicted AQI as Int
            output[0][0].toInt()
        }
    }

    fun close() {
        interpreter?.close()
    }
}