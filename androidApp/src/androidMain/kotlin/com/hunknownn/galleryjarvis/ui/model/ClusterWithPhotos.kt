package com.hunknownn.galleryjarvis.ui.model

/**
 * UI에서 사용하는 클러스터 표시 데이터.
 *
 * @param representativePhotoUri 클러스터 대표 사진의 content:// URI (썸네일용)
 * @param photoUris 클러스터에 속한 모든 사진의 content:// URI 목록
 */
data class ClusterWithPhotos(
    val clusterId: Long,
    val name: String,
    val representativePhotoUri: String?,
    val photoCount: Int,
    val photoUris: List<String> = emptyList()
)
