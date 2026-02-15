package com.hunknownn.galleryjarvis.database

import app.cash.sqldelight.db.SqlDriver

/**
 * SQLDelight 드라이버 생성을 위한 플랫폼 추상화.
 *
 * - Android: AndroidSqliteDriver
 * - iOS: NativeSqliteDriver
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
