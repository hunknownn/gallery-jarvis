package com.hunknownn.galleryjarvis.ui.viewmodel

import android.content.Context
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
import kotlinx.coroutines.yield
import java.util.Calendar

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
    private val nameGenerator = ServiceLocator.nameGenerator
    private val imageLabeler = ServiceLocator.imageLabeler
    private val backgroundScheduler = ServiceLocator.backgroundTaskScheduler

    private val prefs = platformContext.context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _clusters = MutableStateFlow<List<ClusterWithPhotos>>(emptyList())
    val clusters: StateFlow<List<ClusterWithPhotos>> = _clusters.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress.asStateFlow()

    private val _processedCount = MutableStateFlow(0)
    val processedCount: StateFlow<Int> = _processedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _autoClassifyEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_CLASSIFY, false)
    )
    val autoClassifyEnabled: StateFlow<Boolean> = _autoClassifyEnabled.asStateFlow()

    private var observerRegistered = false

    companion object {
        private const val BATCH_SIZE = 50
        private const val PREFS_NAME = "gallery_jarvis_prefs"
        private const val KEY_AUTO_CLASSIFY = "auto_classify_enabled"
    }

    init {
        loadClusters()
        if (_autoClassifyEnabled.value) {
            observeGalleryChanges()
            scanAndClassify()
            backgroundScheduler.scheduleEmbeddingExtraction()
            backgroundScheduler.scheduleBatchClustering()
        }
    }

    /**
     * 자동 분류 ON/OFF를 토글한다.
     * ON: ContentObserver 등록 + 미처리분 자동 스캔
     * OFF: ContentObserver 해제
     */
    fun toggleAutoClassify() {
        val newValue = !_autoClassifyEnabled.value
        _autoClassifyEnabled.value = newValue
        prefs.edit().putBoolean(KEY_AUTO_CLASSIFY, newValue).apply()

        if (newValue) {
            observeGalleryChanges()
            scanAndClassify()
            // 앱 종료 후에도 백그라운드 처리가 이어지도록 WorkManager 스케줄링
            backgroundScheduler.scheduleEmbeddingExtraction()
            backgroundScheduler.scheduleBatchClustering()
        } else {
            backgroundScheduler.cancelAll()
        }
    }

    /**
     * 갤러리 사진을 스캔하고, 배치 단위(50장)로 임베딩 추출 및 클러스터링을 수행한다.
     * 이미 처리된 사진(embedding_path 존재)은 건너뛰므로 중단 후 재진입 시 이어서 처리된다.
     */
    fun scanAndClassify() {
        if (_isProcessing.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _processedCount.value = 0
            _totalCount.value = 0
            try {
                val photos = scanner.scanAllPhotos()
                _progress.value = "사진 ${photos.size}장 발견"

                var processed = 0
                val total = photos.size
                _totalCount.value = total

                for (batch in photos.chunked(BATCH_SIZE)) {
                    for (photo in batch) {
                        yield() // 코루틴 취소 지점

                        // 이미 처리된 사진은 건너뜀
                        val existing = db.photosQueries.findById(photo.id).executeAsOneOrNull()
                        if (existing?.embedding_path != null) {
                            processed++
                            _processedCount.value = processed
                            continue
                        }

                        try {
                            // DB에 사진 메타데이터 저장
                            val metadata = scanner.getPhotoMetadata(photo.id) ?: run {
                                processed++
                                continue
                            }
                            db.photosQueries.insert(
                                metadata.id, metadata.platformUri, metadata.dateTaken,
                                metadata.latitude, metadata.longitude, metadata.mimeType,
                                metadata.hash, metadata.embeddingPath
                            )

                            // 임베딩 추출
                            val imageBytes = ImageLoader.loadImageBytes(platformContext, photo.platformUri) ?: run {
                                processed++
                                continue
                            }
                            val embedding = extractor.extractEmbedding(imageBytes)

                            // 임베딩 캐시 저장
                            val embeddingPath = "${fileStorage.getEmbeddingCacheDir()}/photo_${photo.id}.bin"
                            fileStorage.saveFile(embeddingPath, EmbeddingSerializer.serialize(embedding))
                            db.photosQueries.updateEmbeddingPath(embeddingPath, photo.id)

                            // 클러스터에 할당
                            clustering.assignPhoto(photo.id, embedding)
                        } catch (e: OutOfMemoryError) {
                            android.util.Log.e("GalleryViewModel", "OOM: 사진 ${photo.id} 건너뜀", e)
                            System.gc()
                        }

                        processed++
                        _processedCount.value = processed
                        _progress.value = "처리 중... $processed/$total"
                    }

                    // 배치 완료마다 UI에 반영 + GC 유도
                    loadClustersSync()
                    System.gc()
                }

                // 클러스터 이름 자동 생성
                generateClusterNames()

                loadClustersSync()
                _progress.value = "완료"
            } catch (e: Exception) {
                _progress.value = "오류: ${e.message}"
            } finally {
                _isProcessing.value = false
                _processedCount.value = 0
                _totalCount.value = 0
            }
        }
    }

    /**
     * 클러스터 이름을 사용자 입력값으로 변경한다.
     */
    fun updateClusterName(clusterId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            db.clustersQueries.updateName(newName, now, clusterId)
            loadClustersSync()
        }
    }

    /**
     * DB에서 클러스터 목록을 로드하여 UI 상태를 갱신한다.
     */
    fun loadClusters() {
        viewModelScope.launch(Dispatchers.IO) {
            loadClustersSync()
        }
    }

    /**
     * Pull-to-refresh: DB에서 클러스터 목록을 리로드한다.
     */
    fun refreshClusters() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                loadClustersSync()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * 동기적으로 클러스터 목록을 로드. IO 스레드에서 호출해야 한다.
     */
    private fun loadClustersSync() {
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

    /**
     * 이름이 없는 클러스터에 날짜·라벨 기반 이름을 자동 생성한다.
     * 대표 사진 1장만 분류하여 카테고리 라벨을 추출한다.
     */
    private fun generateClusterNames() {
        val clusterList = db.clustersQueries.selectAll().executeAsList()
        val now = System.currentTimeMillis()

        for (cluster in clusterList) {
            // 이미 이름이 있으면 건너뜀
            if (cluster.name != null) continue

            val photos = db.photosQueries.selectByClusterId(cluster.cluster_id).executeAsList()
            val dateRange = extractDateRange(photos.mapNotNull { it.date_taken })

            // 대표 사진에서 카테고리 라벨 추출 (클러스터당 1회만 추론)
            val label = classifyRepresentativePhoto(cluster.representative_photo_id, photos)

            val generatedName = nameGenerator.generateName(
                label = label,
                dateRange = dateRange,
                location = null
            )

            db.clustersQueries.updateName(generatedName, now, cluster.cluster_id)
        }
    }

    /**
     * 대표 사진을 분류하여 한국어 카테고리 라벨을 반환한다.
     * 대표 사진이 없으면 첫 번째 사진을 사용한다.
     */
    private fun classifyRepresentativePhoto(
        representativePhotoId: String?,
        photos: List<com.hunknownn.galleryjarvis.Photos>
    ): String? {
        if (photos.isEmpty()) return null

        val targetUri = if (representativePhotoId != null) {
            photos.find { it.photo_id == representativePhotoId }?.platform_uri
        } else {
            null
        } ?: photos.first().platform_uri

        val imageBytes = ImageLoader.loadImageBytes(platformContext, targetUri) ?: return null
        return imageLabeler.classifyImage(imageBytes)
    }

    /**
     * 타임스탬프 목록에서 날짜 범위 문자열을 추출한다.
     * 예: "2024.06", "2024.03 ~ 2024.06"
     */
    private fun extractDateRange(timestamps: List<Long>): String? {
        if (timestamps.isEmpty()) return null

        val cal = Calendar.getInstance()

        data class YearMonth(val year: Int, val month: Int)

        val yearMonths = timestamps.map { ts ->
            cal.timeInMillis = ts
            YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
        val earliest = yearMonths.minBy { it.year * 100 + it.month }
        val latest = yearMonths.maxBy { it.year * 100 + it.month }

        val startStr = "${earliest.year}.${earliest.month.toString().padStart(2, '0')}"
        val endStr = "${latest.year}.${latest.month.toString().padStart(2, '0')}"

        return if (startStr == endStr) startStr else "$startStr ~ $endStr"
    }

    /**
     * 갤러리 변경을 감지하여 새 사진을 증분 처리한다.
     */
    private fun observeGalleryChanges() {
        if (observerRegistered) return
        observerRegistered = true

        scanner.observeChanges { changedIds ->
            if (!_autoClassifyEnabled.value) return@observeChanges

            viewModelScope.launch(Dispatchers.IO) {
                for (photoId in changedIds) {
                    val existing = db.photosQueries.findById(photoId).executeAsOneOrNull()
                    if (existing?.embedding_path != null) continue

                    val metadata = scanner.getPhotoMetadata(photoId) ?: continue
                    db.photosQueries.insert(
                        metadata.id, metadata.platformUri, metadata.dateTaken,
                        metadata.latitude, metadata.longitude, metadata.mimeType,
                        metadata.hash, metadata.embeddingPath
                    )

                    val imageBytes = ImageLoader.loadImageBytes(platformContext, metadata.platformUri) ?: continue
                    val embedding = extractor.extractEmbedding(imageBytes)

                    val embeddingPath = "${fileStorage.getEmbeddingCacheDir()}/photo_${photoId}.bin"
                    fileStorage.saveFile(embeddingPath, EmbeddingSerializer.serialize(embedding))
                    db.photosQueries.updateEmbeddingPath(embeddingPath, photoId)

                    clustering.assignPhoto(photoId, embedding)
                }
                loadClustersSync()
            }
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
