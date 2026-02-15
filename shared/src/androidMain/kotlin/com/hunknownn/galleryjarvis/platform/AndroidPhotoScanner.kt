package com.hunknownn.galleryjarvis.platform

import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.hunknownn.galleryjarvis.model.PhotoIdentifier
import com.hunknownn.galleryjarvis.model.PhotoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android MediaStore 기반 사진 스캐너.
 *
 * ContentResolver를 통해 갤러리 사진을 스캔하고,
 * ContentObserver로 사진 추가/삭제 변경을 감지한다.
 */
actual class PhotoScanner(
    private val platformContext: PlatformContext
) {
    private val contentResolver get() = platformContext.context.contentResolver
    private var contentObserver: ContentObserver? = null

    /**
     * 갤러리의 모든 사진을 스캔하여 식별자 목록을 반환한다.
     * 촬영일 기준 내림차순(최신순) 정렬.
     */
    actual suspend fun scanAllPhotos(): List<PhotoIdentifier> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<PhotoIdentifier>()
        val projection = arrayOf(MediaStore.Images.Media._ID)

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                photos.add(PhotoIdentifier(id = id.toString(), platformUri = uri.toString()))
            }
        }
        photos
    }

    /**
     * 특정 사진의 상세 메타데이터를 조회한다.
     *
     * @param id MediaStore ID (String)
     * @return 메타데이터 또는 null (해당 ID의 사진이 없는 경우)
     */
    actual suspend fun getPhotoMetadata(id: String): PhotoMetadata? = withContext(Dispatchers.IO) {
        val uri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toLong()
        )
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.MIME_TYPE,
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dateTaken = cursor.getLong(
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                )
                val mimeType = cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                )

                PhotoMetadata(
                    id = id,
                    platformUri = uri.toString(),
                    dateTaken = if (dateTaken > 0) dateTaken else null,
                    latitude = null,
                    longitude = null,
                    mimeType = mimeType,
                    hash = null,
                    embeddingPath = null
                )
            } else null
        }
    }

    /**
     * 갤러리 변경(사진 추가/삭제)을 감지하여 콜백을 호출한다.
     * 변경 발생 시 전체 사진 ID 목록이 아닌, 변경 알림만 전달한다.
     */
    actual fun observeChanges(callback: (List<String>) -> Unit) {
        contentObserver?.let { contentResolver.unregisterContentObserver(it) }

        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                val changedId = uri?.lastPathSegment
                callback(listOfNotNull(changedId))
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )
    }
}
