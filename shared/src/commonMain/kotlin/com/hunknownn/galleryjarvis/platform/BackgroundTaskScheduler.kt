package com.hunknownn.galleryjarvis.platform

expect class BackgroundTaskScheduler {
    fun scheduleEmbeddingExtraction()
    fun scheduleBatchClustering()
    fun cancelAll()
}
