package com.hunknownn.galleryjarvis.clustering

import com.hunknownn.galleryjarvis.platform.FileStorage
import com.hunknownn.galleryjarvis.util.EmbeddingSerializer

class IncrementalClustering(
    private val fileStorage: FileStorage,
    private val threshold: Float = 0.5f
) {
    // TODO: DB 연동 후 구현
    // suspend fun assignPhoto(photoId: String, embedding: FloatArray) { ... }
}
