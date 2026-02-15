package com.hunknownn.galleryjarvis.platform

actual class BackgroundTaskScheduler {

    actual fun scheduleEmbeddingExtraction() {
        // TODO: WorkManager OneTimeWorkRequest 등록
    }

    actual fun scheduleBatchClustering() {
        // TODO: WorkManager PeriodicWorkRequest 등록
    }

    actual fun cancelAll() {
        // TODO: WorkManager.cancelAllWork()
    }
}
