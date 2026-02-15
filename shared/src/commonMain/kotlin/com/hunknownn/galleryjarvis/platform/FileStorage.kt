package com.hunknownn.galleryjarvis.platform

/**
 * 임베딩 캐시 파일 I/O를 위한 플랫폼 추상화.
 *
 * - Android: 앱 내부 캐시 디렉토리 (`/cache/embeddings/`)
 * - iOS: `Library/Caches/embeddings/`
 */
expect class FileStorage {
    fun getEmbeddingCacheDir(): String
    fun saveFile(path: String, data: ByteArray)
    fun loadFile(path: String): ByteArray?
    fun deleteFile(path: String)
}
