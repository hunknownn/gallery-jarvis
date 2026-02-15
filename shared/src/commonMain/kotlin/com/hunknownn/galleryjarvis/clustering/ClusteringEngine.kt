package com.hunknownn.galleryjarvis.clustering

import kotlin.math.sqrt

/**
 * 클러스터링 연산에 사용하는 유틸리티 함수 모음.
 */
object ClusteringEngine {

    /**
     * 두 임베딩 벡터 간의 코사인 거리를 계산한다.
     *
     * @return 0.0(동일) ~ 2.0(정반대) 범위의 거리 값. 영벡터인 경우 1.0 반환.
     */
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

    /**
     * 새 임베딩이 추가된 후 클러스터 중심 벡터를 갱신한다.
     *
     * 공식: `new_center = old_center * (n/(n+1)) + embedding * (1/(n+1))`
     *
     * @param clusterSize 새 임베딩 추가 전 기존 클러스터의 사진 수
     */
    fun updateCenter(oldCenter: FloatArray, newEmbedding: FloatArray, clusterSize: Int): FloatArray {
        val n = clusterSize.toFloat()
        return FloatArray(oldCenter.size) { i ->
            oldCenter[i] * (n / (n + 1f)) + newEmbedding[i] * (1f / (n + 1f))
        }
    }
}
