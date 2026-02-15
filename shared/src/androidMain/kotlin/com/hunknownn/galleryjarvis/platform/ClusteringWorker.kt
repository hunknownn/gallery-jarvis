package com.hunknownn.galleryjarvis.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hunknownn.galleryjarvis.di.ServiceLocator
import com.hunknownn.galleryjarvis.util.EmbeddingSerializer

/**
 * 백그라운드에서 미분류 사진을 클러스터에 할당하는 Worker.
 *
 * 임베딩은 있지만 클러스터에 할당되지 않은 사진을 찾아
 * IncrementalClustering으로 클러스터에 배정한다.
 */
class ClusteringWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = ServiceLocator.database
            val fileStorage = ServiceLocator.fileStorage
            val clustering = ServiceLocator.incrementalClustering

            // 임베딩이 있는 모든 사진 조회
            val allPhotos = db.photosQueries.selectAll().executeAsList()

            for (photo in allPhotos) {
                if (isStopped) break

                val embeddingPath = photo.embedding_path ?: continue

                // 이미 클러스터에 할당된 사진은 건너뜀
                val existing = db.photo_cluster_mapQueries.findByPhotoId(photo.photo_id).executeAsList()
                if (existing.isNotEmpty()) continue

                val embeddingBytes = fileStorage.loadFile(embeddingPath) ?: continue
                val embedding = EmbeddingSerializer.deserialize(embeddingBytes)
                clustering.assignPhoto(photo.photo_id, embedding)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ClusteringWorker", "클러스터링 실패", e)
            Result.failure()
        }
    }
}
