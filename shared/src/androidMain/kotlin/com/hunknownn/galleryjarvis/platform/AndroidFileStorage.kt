package com.hunknownn.galleryjarvis.platform

actual class FileStorage {

    actual fun getEmbeddingCacheDir(): String {
        // TODO: Context.cacheDir 기반 경로 반환
        return ""
    }

    actual fun saveFile(path: String, data: ByteArray) {
        // TODO: java.io.File로 저장
    }

    actual fun loadFile(path: String): ByteArray? {
        // TODO: java.io.File로 읽기
        return null
    }

    actual fun deleteFile(path: String) {
        // TODO: java.io.File 삭제
    }
}
