package com.hunknownn.galleryjarvis.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(
    private val context: android.content.Context
) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            GalleryJarvisDatabase.Schema,
            context,
            "galleryjarvis.db"
        )
    }
}
