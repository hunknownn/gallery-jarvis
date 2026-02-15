package com.hunknownn.galleryjarvis.database

/**
 * [GalleryJarvisDatabase] 인스턴스 생성 팩토리.
 */
object AppDatabaseFactory {

    /** 플랫폼별 [DatabaseDriverFactory]로부터 DB 인스턴스를 생성한다. */
    fun create(driverFactory: DatabaseDriverFactory): GalleryJarvisDatabase {
        val driver = driverFactory.createDriver()
        return GalleryJarvisDatabase(driver)
    }
}
