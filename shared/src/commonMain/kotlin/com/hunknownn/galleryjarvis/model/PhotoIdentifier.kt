package com.hunknownn.galleryjarvis.model

/**
 * 플랫폼 독립적인 사진 식별자.
 *
 * @param id 사진 고유 ID (Android: MediaStore ID를 String 변환 / iOS: PHAsset localIdentifier)
 * @param platformUri 사진 접근 URI (Android: content:// URI / iOS: localIdentifier)
 */
data class PhotoIdentifier(
    val id: String,
    val platformUri: String
)
