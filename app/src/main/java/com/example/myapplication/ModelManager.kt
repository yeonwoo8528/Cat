package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import java.io.BufferedReader
import java.io.InputStreamReader

class ModelManager(private val context: Context) {
    private val classifiers = mutableMapOf<String, Classifier>()
    private val diseaseMap = mutableMapOf<String, MutableList<String>>()
    private val modelNames = mutableListOf<String>()

    init {
        modelNames.add("model_a")
        modelNames.add("model_b")
        modelNames.add("model_c")

        for (modelName in modelNames) {
            classifiers[modelName] = Classifier(context, "$modelName.tflite")
        }

        val diseaseFile = context.assets.open("disease.txt")
        val reader = BufferedReader(InputStreamReader(diseaseFile))

        var line: String?
        var currentModel = ""
        while (reader.readLine().also { line = it } != null) {
            if (line!!.startsWith("Model:")) {
                currentModel = line!!.substringAfter("Model:").trim()
                diseaseMap[currentModel] = mutableListOf()
            } else {
                diseaseMap[currentModel]?.add(line!!.trim())
            }
        }

        reader.close()
    }

    fun initModels() {
        classifiers.values.forEach { it.init() }
    }

    fun classifyImage(image: Bitmap): List<Pair<String, Float>> {
        val allResults = mutableListOf<Pair<String, Float>>()

        for (modelName in modelNames) {
            val classifier = classifiers[modelName] ?: continue
            val result = classifier.classify(image)

            if (result.first == "Y") {
                val diseaseName = diseaseMap[modelName]?.get(0) ?: "정상"
                allResults.add(Pair(diseaseName, result.second))
            }
        }

        return allResults
    }

}