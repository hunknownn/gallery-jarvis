package com.hunknownn.galleryjarvis.model

data class Cluster(
    val clusterId: Long,
    val name: String?,
    val representativePhotoId: String?,
    val centerEmbeddingPath: String?,
    val createdAt: Long,
    val updatedAt: Long
)
