package com.hunknownn.galleryjarvis.di

import com.hunknownn.galleryjarvis.clustering.IncrementalClustering
import com.hunknownn.galleryjarvis.database.GalleryJarvisDatabase
import com.hunknownn.galleryjarvis.naming.NameGenerator
import com.hunknownn.galleryjarvis.platform.BackgroundTaskScheduler
import com.hunknownn.galleryjarvis.platform.EmbeddingExtractor
import com.hunknownn.galleryjarvis.platform.FileStorage
import com.hunknownn.galleryjarvis.platform.ImageLabeler
import com.hunknownn.galleryjarvis.platform.PhotoScanner

/**
 * 앱 전역 의존성 관리를 위한 Service Locator.
 *
 * Application 초기화 시 [initialize]를 호출하여 모든 서비스를 등록한다.
 * WorkManager Worker 등 시스템이 생성하는 컴포넌트에서 서비스에 접근할 때 사용.
 */
object ServiceLocator {

    lateinit var database: GalleryJarvisDatabase
        private set

    lateinit var fileStorage: FileStorage
        private set

    lateinit var photoScanner: PhotoScanner
        private set

    lateinit var embeddingExtractor: EmbeddingExtractor
        private set

    lateinit var imageLabeler: ImageLabeler
        private set

    lateinit var backgroundTaskScheduler: BackgroundTaskScheduler
        private set

    lateinit var incrementalClustering: IncrementalClustering
        private set

    lateinit var nameGenerator: NameGenerator
        private set

    /**
     * 모든 서비스를 초기화한다. Application.onCreate에서 호출해야 한다.
     */
    fun initialize(
        database: GalleryJarvisDatabase,
        fileStorage: FileStorage,
        photoScanner: PhotoScanner,
        embeddingExtractor: EmbeddingExtractor,
        imageLabeler: ImageLabeler,
        backgroundTaskScheduler: BackgroundTaskScheduler,
    ) {
        this.database = database
        this.fileStorage = fileStorage
        this.photoScanner = photoScanner
        this.embeddingExtractor = embeddingExtractor
        this.imageLabeler = imageLabeler
        this.backgroundTaskScheduler = backgroundTaskScheduler
        this.incrementalClustering = IncrementalClustering(database, fileStorage)
        this.nameGenerator = NameGenerator()
    }
}
