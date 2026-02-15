package com.hunknownn.galleryjarvis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunknownn.galleryjarvis.di.ServiceLocator
import com.hunknownn.galleryjarvis.platform.ImageLoader
import com.hunknownn.galleryjarvis.platform.PlatformContext
import com.hunknownn.galleryjarvis.ui.model.ClusterWithPhotos
import com.hunknownn.galleryjarvis.util.EmbeddingSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 갤러리 화면의 상태를 관리하는 ViewModel.
 *
 * 사진 스캔 → 임베딩 추출 → 클러스터링 → UI 상태 업데이트 플로우를 조율한다.
 */
class GalleryViewModel(
    private val platformContext: PlatformContext
) : ViewModel() {

    private val db = ServiceLocator.database
    private val scanner = ServiceLocator.photoScanner
    private val extractor = ServiceLocator.embeddingExtractor
    private val fileStorage = ServiceLocator.fileStorage
    private val clustering = ServiceLocator.incrementalClustering

    private val _clusters = MutableStateFlow<List<ClusterWithPhotos>>(emptyList())
    val clusters: StateFlow<List<ClusterWithPhotos>> = _clusters.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress.asStateFlow()

    init {
        loadClusters()
    }

    /**
     * 갤러리 사진을 스캔하고, 임베딩 추출 및 클러스터링을 수행한다.
     */
    fun scanAndClassify() {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val photos = scanner.scanAllPhotos()
                _progress.value = "사진 ${photos.size}장 발견"

                var processed = 0
                for (photo in photos) {
                    // 이미 처리된 사진은 건너뜀
                    val existing = db.photosQueries.findById(photo.id).executeAsOneOrNull()
                    if (existing?.embedding_path != null) {
                        processed++
                        continue
                    }

                    // DB에 사진 메타데이터 저장
                    val metadata = scanner.getPhotoMetadata(photo.id) ?: continue
                    db.photosQueries.insert(
                        metadata.id, metadata.platformUri, metadata.dateTaken,
                        metadata.latitude, metadata.longitude, metadata.mimeType,
                        metadata.hash, metadata.embeddingPath
                    )

                    // 임베딩 추출
                    val imageBytes = ImageLoader.loadImageBytes(platformContext, photo.platformUri) ?: continue
                    val embedding = extractor.extractEmbedding(imageBytes)

                    // 임베딩 캐시 저장
                    val embeddingPath = "${fileStorage.getEmbeddingCacheDir()}/photo_${photo.id}.bin"
                    fileStorage.saveFile(embeddingPath, EmbeddingSerializer.serialize(embedding))
                    db.photosQueries.updateEmbeddingPath(embeddingPath, photo.id)

                    // 클러스터에 할당
                    clustering.assignPhoto(photo.id, embedding)

                    processed++
                    _progress.value = "처리 중... $processed/${photos.size}"
                }

                loadClusters()
                _progress.value = "완료"
            } catch (e: Exception) {
                _progress.value = "오류: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * DB에서 클러스터 목록을 로드하여 UI 상태를 갱신한다.
     */
    fun loadClusters() {
        viewModelScope.launch(Dispatchers.IO) {
            val clusterList = db.clustersQueries.selectAll().executeAsList()
            val result = clusterList.map { cluster ->
                val photos = db.photosQueries.selectByClusterId(cluster.cluster_id).executeAsList()
                val photoUris = photos.map { it.platform_uri }

                ClusterWithPhotos(
                    clusterId = cluster.cluster_id,
                    name = cluster.name ?: "그룹 ${cluster.cluster_id}",
                    representativePhotoUri = cluster.representative_photo_id?.let { repId ->
                        photos.find { it.photo_id == repId }?.platform_uri
                    } ?: photoUris.firstOrNull(),
                    photoCount = photos.size,
                    photoUris = photoUris
                )
            }
            _clusters.value = result
        }
    }

    /**
     * 특정 클러스터의 사진 URI 목록을 반환한다.
     */
    fun getClusterPhotos(clusterId: Long): List<String> {
        return _clusters.value
            .find { it.clusterId == clusterId }
            ?.photoUris ?: emptyList()
    }
}
