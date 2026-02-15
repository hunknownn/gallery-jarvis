package com.hunknownn.galleryjarvis.model

/**
 * 사진 클러스터(자동 생성된 앨범 그룹) 정보.
 *
 * @param centerEmbeddingPath 클러스터 중심 임베딩 벡터가 저장된 바이너리 파일 경로
 * @param representativePhotoId 클러스터를 대표하는 사진 ID (center와 가장 가까운 사진)
 */
data class Cluster(
    val clusterId: Long,
    val name: String?,
    val representativePhotoId: String?,
    val centerEmbeddingPath: String?,
    val createdAt: Long,
    val updatedAt: Long
)
