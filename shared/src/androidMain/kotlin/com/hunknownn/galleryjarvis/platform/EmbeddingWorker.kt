package com.hunknownn.galleryjarvis.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hunknownn.galleryjarvis.di.ServiceLocator
import com.hunknownn.galleryjarvis.util.EmbeddingSerializer

/**
 * 백그라운드에서 미처리 사진의 임베딩을 추출하는 Worker.
 *
 * DB에서 embedding_path가 null인 사진을 조회하여 순차적으로 임베딩을 추출하고,
 * 캐시 파일로 저장한 뒤 DB에 경로를 업데이트한다.
 */
class EmbeddingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = ServiceLocator.database
        val extractor = ServiceLocator.embeddingExtractor
        val fileStorage = ServiceLocator.fileStorage
        val platformContext = PlatformContext(applicationContext)

        val photosWithoutEmbedding = db.photosQueries.selectWithoutEmbedding().executeAsList()

        for (photo in photosWithoutEmbedding) {
            val imageBytes = ImageLoader.loadImageBytes(platformContext, photo.platform_uri) ?: continue
            val embedding = extractor.extractEmbedding(imageBytes)

            val embeddingPath = "${fileStorage.getEmbeddingCacheDir()}/photo_${photo.photo_id}.bin"
            fileStorage.saveFile(embeddingPath, EmbeddingSerializer.serialize(embedding))
            db.photosQueries.updateEmbeddingPath(embeddingPath, photo.photo_id)
        }

        return Result.success()
    }
}
