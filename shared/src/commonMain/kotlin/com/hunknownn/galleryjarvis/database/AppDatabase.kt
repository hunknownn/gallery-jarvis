package com.hunknownn.galleryjarvis.database

object AppDatabaseFactory {

    fun create(driverFactory: DatabaseDriverFactory): GalleryJarvisDatabase {
        val driver = driverFactory.createDriver()
        return GalleryJarvisDatabase(driver)
    }
}
