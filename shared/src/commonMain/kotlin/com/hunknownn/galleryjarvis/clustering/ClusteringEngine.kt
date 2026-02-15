package com.hunknownn.galleryjarvis.clustering

import kotlin.math.sqrt

object ClusteringEngine {

    fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 1f else 1f - (dot / denom)
    }

    fun updateCenter(oldCenter: FloatArray, newEmbedding: FloatArray, clusterSize: Int): FloatArray {
        val n = clusterSize.toFloat()
        return FloatArray(oldCenter.size) { i ->
            oldCenter[i] * (n / (n + 1f)) + newEmbedding[i] * (1f / (n + 1f))
        }
    }
}
