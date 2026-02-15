package com.hunknownn.galleryjarvis.clustering

import kotlin.math.*

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

    /**
     * L2 정규화: 벡터 길이를 1로 만든다. 영벡터는 그대로 반환.
     * 이미 정규화된 벡터에 재적용해도 결과 동일 (멱등성).
     */
    fun l2Normalize(v: FloatArray): FloatArray {
        var sumSq = 0f
        for (x in v) sumSq += x * x
        val norm = sqrt(sumSq)
        if (norm == 0f) return v
        return FloatArray(v.size) { i -> v[i] / norm }
    }

    /**
     * 중심 벡터를 갱신한 후 L2 정규화를 적용한다.
     */
    fun updateCenterNormalized(oldCenter: FloatArray, newEmbedding: FloatArray, clusterSize: Int): FloatArray {
        return l2Normalize(updateCenter(oldCenter, newEmbedding, clusterSize))
    }

    /**
     * 두 사진의 촬영 시각 차이를 0~1 거리로 변환한다 (시그모이드 커브).
     * 3일 이내 → ~0.12, 7일 → 0.5, 14일+ → ~0.97.
     * 어느 한쪽이 null이면 0.5(중립) 반환.
     */
    fun timeDistance(dateTakenA: Long?, dateTakenB: Long?): Float {
        if (dateTakenA == null || dateTakenB == null) return 0.5f
        val diffDays = abs(dateTakenA - dateTakenB).toFloat() / (1000f * 60f * 60f * 24f)
        // 시그모이드: 1 / (1 + exp(-k*(x - mid))), k=0.5, mid=7
        return (1.0 / (1.0 + exp(-0.5 * (diffDays - 7.0)))).toFloat()
    }

    /**
     * 두 GPS 좌표 간 거리를 0~1 거리로 변환한다.
     * Haversine으로 km 계산 후 시그모이드 정규화.
     * 1km → ~0.05, 20km → 0.5, 100km+ → ~0.95.
     * 어느 한쪽이 null이면 0.5(중립) 반환.
     */
    fun gpsDistance(latA: Double?, lngA: Double?, latB: Double?, lngB: Double?): Float {
        if (latA == null || lngA == null || latB == null || lngB == null) return 0.5f
        val km = haversineKm(latA, lngA, latB, lngB)
        // 시그모이드: 1 / (1 + exp(-k*(x - mid))), k=0.1, mid=20
        return (1.0 / (1.0 + exp(-0.1 * (km - 20.0)))).toFloat()
    }

    /**
     * 복합 거리: 임베딩 60% + 시간 25% + GPS 15%.
     */
    fun combinedDistance(embeddingDist: Float, timeDist: Float, gpsDist: Float): Float {
        val clampedEmb = embeddingDist.coerceIn(0f, 1f)
        return clampedEmb * 0.60f + timeDist * 0.25f + gpsDist * 0.15f
    }

    private fun toRadians(deg: Double): Double = deg * PI / 180.0

    /**
     * Haversine 공식으로 두 GPS 좌표 간 거리(km)를 계산한다.
     */
    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // 지구 반지름 (km)
        val dLat = toRadians(lat2 - lat1)
        val dLng = toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
