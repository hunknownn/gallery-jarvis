package com.hunknownn.galleryjarvis.platform

expect class EmbeddingExtractor {
    fun extractEmbedding(imageData: ByteArray): FloatArray
    fun close()
}
