package com.hunknownn.galleryjarvis.platform

import com.hunknownn.galleryjarvis.model.PhotoIdentifier
import com.hunknownn.galleryjarvis.model.PhotoMetadata

/**
 * 기기 갤러리의 사진을 스캔하고 변경을 감지하는 플랫폼 추상화.
 *
 * - Android: MediaStore ContentResolver 기반
 * - iOS: PHFetchResult / PHPhotoLibraryChangeObserver 기반
 */
expect class PhotoScanner {
    suspend fun scanAllPhotos(): List<PhotoIdentifier>
    suspend fun getPhotoMetadata(id: String): PhotoMetadata?
    fun observeChanges(callback: (List<String>) -> Unit)
    fun stopObserving()
}
