package com.hunknownn.galleryjarvis.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            GalleryJarvisDatabase.Schema,
            "galleryjarvis.db"
        )
    }
}
