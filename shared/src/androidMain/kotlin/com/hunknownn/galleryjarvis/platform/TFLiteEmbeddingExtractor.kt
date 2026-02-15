package com.hunknownn.galleryjarvis.platform

actual class EmbeddingExtractor {

    actual fun extractEmbedding(imageData: ByteArray): FloatArray {
        // TODO: TFLite Interpreter를 사용하여 임베딩 추출
        return FloatArray(0)
    }

    actual fun close() {
        // TODO: TFLite Interpreter 해제
    }
}
