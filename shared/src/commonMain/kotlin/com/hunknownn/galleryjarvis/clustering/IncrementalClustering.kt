package com.hunknownn.galleryjarvis.clustering

import com.hunknownn.galleryjarvis.database.GalleryJarvisDatabase
import com.hunknownn.galleryjarvis.platform.FileStorage
import com.hunknownn.galleryjarvis.util.EmbeddingSerializer

/**
 * 점진적(incremental) 클러스터링 엔진.
 *
 * 새 사진이 추가될 때마다 전체 클러스터를 재계산하지 않고,
 * 기존 클러스터 center와의 코사인 거리를 비교하여 할당하거나 새 클러스터를 생성한다.
 *
 * @param threshold 기존 클러스터에 할당하기 위한 최대 코사인 거리 (기본값 0.5)
 */
class IncrementalClustering(
    private val database: GalleryJarvisDatabase,
    private val fileStorage: FileStorage,
    private val threshold: Float = 0.5f
) {
    // TODO: DB 연동 후 구현
    // suspend fun assignPhoto(photoId: String, embedding: FloatArray) { ... }
}
