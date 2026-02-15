package com.hunknownn.galleryjarvis

import android.app.Application
import com.hunknownn.galleryjarvis.database.AppDatabaseFactory
import com.hunknownn.galleryjarvis.database.DatabaseDriverFactory
import com.hunknownn.galleryjarvis.di.ServiceLocator
import com.hunknownn.galleryjarvis.platform.BackgroundTaskScheduler
import com.hunknownn.galleryjarvis.platform.EmbeddingExtractor
import com.hunknownn.galleryjarvis.platform.FileStorage
import com.hunknownn.galleryjarvis.platform.ImageLabeler
import com.hunknownn.galleryjarvis.platform.PhotoScanner
import com.hunknownn.galleryjarvis.platform.PlatformContext

/**
 * 앱 전역 초기화를 담당하는 Application 클래스.
 *
 * [ServiceLocator]에 모든 서비스를 등록하여
 * Activity, ViewModel, Worker 등에서 접근할 수 있도록 한다.
 */
class GalleryJarvisApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val platformContext = PlatformContext(this)
        val driverFactory = DatabaseDriverFactory(this)

        ServiceLocator.initialize(
            database = AppDatabaseFactory.create(driverFactory),
            fileStorage = FileStorage(platformContext),
            photoScanner = PhotoScanner(platformContext),
            embeddingExtractor = EmbeddingExtractor(platformContext),
            imageLabeler = ImageLabeler(platformContext),
            backgroundTaskScheduler = BackgroundTaskScheduler(platformContext),
        )
    }
}
