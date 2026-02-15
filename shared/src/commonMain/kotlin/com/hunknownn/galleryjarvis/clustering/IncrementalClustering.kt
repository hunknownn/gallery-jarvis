package com.hunknownn.galleryjarvis.clustering

import com.hunknownn.galleryjarvis.database.GalleryJarvisDatabase
import com.hunknownn.galleryjarvis.platform.FileStorage
import com.hunknownn.galleryjarvis.util.EmbeddingSerializer
import kotlinx.datetime.Clock

/**
 * 점진적(incremental) 클러스터링 엔진.
 *
 * 새 사진이 추가될 때마다 전체 클러스터를 재계산하지 않고,
 * 기존 클러스터 center와의 코사인 거리를 비교하여 할당하거나 새 클러스터를 생성한다.
 *
 * 흐름:
 * 1. 새 임베딩 생성
 * 2. 기존 cluster center 목록 조회
 * 3. 각 center와 코사인 거리 비교
 * 4. threshold 이하 → 가장 가까운 기존 클러스터에 추가 + center 업데이트
 * 5. threshold 초과 → 새 클러스터 생성
 *
 * @param threshold 기존 클러스터에 할당하기 위한 최대 코사인 거리 (기본값 0.5)
 */
class IncrementalClustering(
    private val database: GalleryJarvisDatabase,
    private val fileStorage: FileStorage,
    private val threshold: Float = 0.5f
) {
    /**
     * 사진을 기존 클러스터에 할당하거나, 새 클러스터를 생성한다.
     *
     * @param photoId 사진 ID (MediaStore ID 또는 PHAsset localIdentifier)
     * @param embedding 해당 사진의 임베딩 벡터
     */
    fun assignPhoto(photoId: String, embedding: FloatArray) {
        val clusters = database.clustersQueries.selectAll().executeAsList()

        if (clusters.isEmpty()) {
            createNewCluster(photoId, embedding)
            return
        }

        // 가장 가까운 클러스터 탐색
        var minDistance = Float.MAX_VALUE
        var nearestClusterId: Long? = null
        var nearestCenterPath: String? = null

        for (cluster in clusters) {
            val centerPath = cluster.center_embedding_path ?: continue
            val centerBytes = fileStorage.loadFile(centerPath) ?: continue
            val centerEmbedding = EmbeddingSerializer.deserialize(centerBytes)
            val distance = ClusteringEngine.cosineDistance(embedding, centerEmbedding)

            if (distance < minDistance) {
                minDistance = distance
                nearestClusterId = cluster.cluster_id
                nearestCenterPath = centerPath
            }
        }

        if (nearestClusterId != null && minDistance < threshold) {
            assignToExistingCluster(photoId, embedding, nearestClusterId, nearestCenterPath!!)
        } else {
            createNewCluster(photoId, embedding)
        }
    }

    /**
     * 기존 클러스터에 사진을 추가하고, center 벡터를 갱신한다.
     */
    private fun assignToExistingCluster(
        photoId: String,
        embedding: FloatArray,
        clusterId: Long,
        centerPath: String
    ) {
        database.transaction {
            database.photo_cluster_mapQueries.insert(photoId, clusterId)

            // center 업데이트: new_center = old * (n/(n+1)) + new * (1/(n+1))
            val clusterSize = database.photo_cluster_mapQueries
                .countByClusterId(clusterId)
                .executeAsOne()

            val oldCenterBytes = fileStorage.loadFile(centerPath)!!
            val oldCenter = EmbeddingSerializer.deserialize(oldCenterBytes)
            // clusterSize는 이미 새 사진 포함이므로 -1
            val newCenter = ClusteringEngine.updateCenter(oldCenter, embedding, (clusterSize - 1).toInt())

            fileStorage.saveFile(centerPath, EmbeddingSerializer.serialize(newCenter))
            database.clustersQueries.updateCenterEmbeddingPath(
                centerPath, nowMillis(), clusterId
            )
        }
    }

    /**
     * 새 클러스터를 생성하고 사진을 할당한다.
     */
    private fun createNewCluster(photoId: String, embedding: FloatArray) {
        val now = nowMillis()
        val centerPath = "${fileStorage.getEmbeddingCacheDir()}/cluster_center_${now}.bin"
        fileStorage.saveFile(centerPath, EmbeddingSerializer.serialize(embedding))

        database.transaction {
            database.clustersQueries.insert(
                name = null,
                representative_photo_id = photoId,
                center_embedding_path = centerPath,
                created_at = now,
                updated_at = now
            )
            val clusterId = database.clustersQueries.lastInsertId().executeAsOne()
            database.photo_cluster_mapQueries.insert(photoId, clusterId)
        }
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
