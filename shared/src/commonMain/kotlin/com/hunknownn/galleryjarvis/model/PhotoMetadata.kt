package com.hunknownn.galleryjarvis.model

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
