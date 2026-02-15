package com.hunknownn.galleryjarvis.platform

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Android WorkManager 기반 백그라운드 작업 스케줄러.
 *
 * - 임베딩 추출: OneTimeWorkRequest (배터리 저전력 아닐 때)
 * - 배치 클러스터링: PeriodicWorkRequest (1시간 주기)
 */
actual class BackgroundTaskScheduler(
    private val platformContext: PlatformContext
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(platformContext.context)

    /**
     * 미처리 사진의 임베딩 추출 작업을 즉시 스케줄링한다.
     * 이미 동일 작업이 진행 중이면 무시(KEEP).
     */
    actual fun scheduleEmbeddingExtraction() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<EmbeddingWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            WORK_EMBEDDING,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /**
     * 미분류 사진의 클러스터링 작업을 주기적으로 스케줄링한다.
     * 1시간 간격으로 실행되며, 배터리 저전력 시 지연.
     */
    actual fun scheduleBatchClustering() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<ClusteringWorker>(
            1, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        workManager.enqueueUniquePeriodicWork(
            WORK_CLUSTERING,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    actual fun cancelAll() {
        workManager.cancelAllWork()
    }

    companion object {
        private const val WORK_EMBEDDING = "embedding_extraction"
        private const val WORK_CLUSTERING = "batch_clustering"
    }
}
