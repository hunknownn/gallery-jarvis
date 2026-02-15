package com.hunknownn.galleryjarvis.platform

import com.hunknownn.galleryjarvis.model.PhotoIdentifier
import com.hunknownn.galleryjarvis.model.PhotoMetadata

actual class PhotoScanner {

    actual suspend fun scanAllPhotos(): List<PhotoIdentifier> {
        // TODO: PHAsset.fetchAssets를 사용하여 사진 스캔
        return emptyList()
    }

    actual suspend fun getPhotoMetadata(id: String): PhotoMetadata? {
        // TODO: PHAsset에서 메타데이터 조회
        return null
    }

    actual fun observeChanges(callback: (List<String>) -> Unit) {
        // TODO: PHPhotoLibraryChangeObserver 등록
    }
}
