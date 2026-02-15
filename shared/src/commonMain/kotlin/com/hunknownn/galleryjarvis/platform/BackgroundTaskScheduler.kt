package com.hunknownn.galleryjarvis.platform

/**
 * 백그라운드 작업 스케줄링을 위한 플랫폼 추상화.
 *
 * - Android: WorkManager (OneTimeWorkRequest / PeriodicWorkRequest)
 * - iOS: BGTaskScheduler (BGProcessingTask / BGAppRefreshTask)
 */
expect class BackgroundTaskScheduler {
    fun scheduleEmbeddingExtraction()
    fun scheduleBatchClustering()
    fun cancelAll()
}
