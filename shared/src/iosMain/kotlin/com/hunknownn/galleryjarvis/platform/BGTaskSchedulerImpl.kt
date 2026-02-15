package com.hunknownn.galleryjarvis.platform

actual class BackgroundTaskScheduler {

    actual fun scheduleEmbeddingExtraction() {
        // TODO: BGProcessingTaskRequest 등록
    }

    actual fun scheduleBatchClustering() {
        // TODO: BGAppRefreshTaskRequest 등록
    }

    actual fun cancelAll() {
        // TODO: BGTaskScheduler.shared.cancelAllTaskRequests()
    }
}
