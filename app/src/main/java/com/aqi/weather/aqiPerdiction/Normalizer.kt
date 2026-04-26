package com.aqi.weather.aqiPerdiction

import com.aqi.weather.aqiPerdiction.models.Scaler
import org.json.JSONObject

object Normalizer {

    fun fromJSON(json: String): Scaler {
        val obj = JSONObject(json)

        val mean = obj.getJSONArray("mean").let {
            List(it.length()) { i -> it.getDouble(i) }
        }

        val scale = obj.getJSONArray("scale").let {
            List(it.length()) { i -> it.getDouble(i) }
        }

        return Scaler(mean, scale)
    }

    fun normalize(input: FloatArray, scaler: Scaler): FloatArray {
        val output = FloatArray(input.size)

        for (i in input.indices) {
            output[i] = ((input[i] - scaler.mean[i]) / scaler.scale[i]).toFloat()
        }

        return output
    }
}