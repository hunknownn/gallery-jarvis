package com.hunknownn.galleryjarvis.platform

/**
 * 이미지를 분류하여 한국어 카테고리 라벨을 반환하는 플랫폼 추상화.
 *
 * - Android: TensorFlow Lite (MobileNetV3 Small classification)
 * - iOS: 미구현 (null 반환)
 *
 * 사용 후 반드시 [close]를 호출하여 리소스를 해제해야 한다.
 */
expect class ImageLabeler {
    /**
     * 이미지를 분류하여 한국어 카테고리 라벨을 반환한다.
     *
     * @param imageData JPEG/PNG 등 이미지 바이너리 데이터
     * @return 한국어 카테고리 라벨 (예: "동물", "음식"). 분류 실패 시 null.
     */
    fun classifyImage(imageData: ByteArray): String?

    fun close()
}
