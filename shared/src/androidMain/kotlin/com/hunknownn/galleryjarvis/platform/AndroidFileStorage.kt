package com.hunknownn.galleryjarvis.platform

import java.io.File

actual class FileStorage(
    private val platformContext: PlatformContext
) {
    private val embeddingDir: File by lazy {
        File(platformContext.context.cacheDir, "embeddings").also { it.mkdirs() }
    }

    actual fun getEmbeddingCacheDir(): String {
        return embeddingDir.absolutePath
    }

    actual fun saveFile(path: String, data: ByteArray) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }

    actual fun loadFile(path: String): ByteArray? {
        val file = File(path)
        return if (file.exists()) file.readBytes() else null
    }

    actual fun deleteFile(path: String) {
        File(path).delete()
    }
}
