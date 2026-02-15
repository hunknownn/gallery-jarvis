package com.hunknownn.galleryjarvis.platform

expect class FileStorage {
    fun getEmbeddingCacheDir(): String
    fun saveFile(path: String, data: ByteArray)
    fun loadFile(path: String): ByteArray?
    fun deleteFile(path: String)
}
