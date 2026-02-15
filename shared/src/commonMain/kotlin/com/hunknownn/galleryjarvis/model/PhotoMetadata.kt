package com.hunknownn.galleryjarvis.model

/**
 * 사진의 상세 메타데이터.
 *
 * 플랫폼별 PhotoScanner에서 조회하여 SQLDelight DB에 저장된다.
 */
data class PhotoMetadata(
    val id: String,
    val platformUri: String,
    val dateTaken: Long?,
    val latitude: Double?,
    val longitude: Double?,
    val mimeType: String?,
    val hash: String?,
    val embeddingPath: String?
)
