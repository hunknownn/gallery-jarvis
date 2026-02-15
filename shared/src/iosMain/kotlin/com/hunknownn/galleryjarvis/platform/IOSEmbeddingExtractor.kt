package com.hunknownn.galleryjarvis.platform

actual class EmbeddingExtractor {

    actual fun extractEmbedding(imageData: ByteArray): FloatArray {
        // TODO: Core ML VNCoreMLRequest를 사용하여 임베딩 추출
        return FloatArray(0)
    }

    actual fun close() {
        // TODO: Core ML 리소스 해제
    }
}
