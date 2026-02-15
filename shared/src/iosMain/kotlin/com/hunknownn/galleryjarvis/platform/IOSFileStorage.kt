package com.hunknownn.galleryjarvis.platform

actual class FileStorage {

    actual fun getEmbeddingCacheDir(): String {
        // TODO: NSCachesDirectory 기반 경로 반환
        return ""
    }

    actual fun saveFile(path: String, data: ByteArray) {
        // TODO: NSFileManager로 저장
    }

    actual fun loadFile(path: String): ByteArray? {
        // TODO: NSFileManager로 읽기
        return null
    }

    actual fun deleteFile(path: String) {
        // TODO: NSFileManager로 삭제
    }
}
