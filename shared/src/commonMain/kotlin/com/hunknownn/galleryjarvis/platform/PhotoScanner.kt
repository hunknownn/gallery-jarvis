package com.hunknownn.galleryjarvis.platform

import com.hunknownn.galleryjarvis.model.PhotoIdentifier
import com.hunknownn.galleryjarvis.model.PhotoMetadata

expect class PhotoScanner {
    suspend fun scanAllPhotos(): List<PhotoIdentifier>
    suspend fun getPhotoMetadata(id: String): PhotoMetadata?
    fun observeChanges(callback: (List<String>) -> Unit)
}
