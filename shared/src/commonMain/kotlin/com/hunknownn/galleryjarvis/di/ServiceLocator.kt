package com.hunknownn.galleryjarvis.di

import com.hunknownn.galleryjarvis.clustering.IncrementalClustering
import com.hunknownn.galleryjarvis.database.GalleryJarvisDatabase
import com.hunknownn.galleryjarvis.platform.BackgroundTaskScheduler
import com.hunknownn.galleryjarvis.platform.EmbeddingExtractor
import com.hunknownn.galleryjarvis.platform.FileStorage
import com.hunknownn.galleryjarvis.platform.PhotoScanner

object ServiceLocator {

    lateinit var database: GalleryJarvisDatabase
        private set

    lateinit var fileStorage: FileStorage
        private set

    lateinit var photoScanner: PhotoScanner
        private set

    lateinit var embeddingExtractor: EmbeddingExtractor
        private set

    lateinit var backgroundTaskScheduler: BackgroundTaskScheduler
        private set

    lateinit var incrementalClustering: IncrementalClustering
        private set

    fun initialize(
        database: GalleryJarvisDatabase,
        fileStorage: FileStorage,
        photoScanner: PhotoScanner,
        embeddingExtractor: EmbeddingExtractor,
        backgroundTaskScheduler: BackgroundTaskScheduler,
    ) {
        this.database = database
        this.fileStorage = fileStorage
        this.photoScanner = photoScanner
        this.embeddingExtractor = embeddingExtractor
        this.backgroundTaskScheduler = backgroundTaskScheduler
        this.incrementalClustering = IncrementalClustering(database, fileStorage)
    }
}
