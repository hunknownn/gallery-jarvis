package com.hunknownn.galleryjarvis.platform

import java.io.File

/**
 * Android 환경의 파일 저장소 구현.
 *
 * 임베딩 벡터 캐시를 앱 내부 캐시 디렉토리(`/cache/embeddings/`)에 바이너리 파일로 관리한다.
 */
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
