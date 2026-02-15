package com.hunknownn.galleryjarvis.platform

actual class ImageLabeler {

    actual fun classifyImage(imageData: ByteArray): String? {
        // TODO: Core ML 기반 이미지 분류 구현
        return null
    }

    actual fun close() {
        // TODO: Core ML 리소스 해제
    }
}
