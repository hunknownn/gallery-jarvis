package com.hunknownn.galleryjarvis.platform

/**
 * 이미지에서 임베딩 벡터를 추출하는 플랫폼 추상화.
 *
 * - Android: TensorFlow Lite (MobileNetV3 Small)
 * - iOS: Core ML
 *
 * 사용 후 반드시 [close]를 호출하여 리소스를 해제해야 한다.
 */
expect class EmbeddingExtractor {
    fun extractEmbedding(imageData: ByteArray): FloatArray
    fun close()
}
