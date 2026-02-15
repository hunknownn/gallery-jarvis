package com.hunknownn.galleryjarvis.platform

import android.net.Uri

/**
 * content:// URI로부터 이미지 바이트를 읽어오는 헬퍼.
 *
 * ContentResolver를 통해 이미지 원본 데이터를 ByteArray로 로드한다.
 * 임베딩 추출 시 EmbeddingExtractor에 전달할 데이터를 준비하는 용도.
 */
object ImageLoader {

    /**
     * content:// URI에서 이미지 바이트를 읽어온다.
     *
     * @param platformContext 플랫폼 컨텍스트
     * @param uriString content:// URI 문자열
     * @return 이미지 바이트 또는 null (읽기 실패 시)
     */
    fun loadImageBytes(platformContext: PlatformContext, uriString: String): ByteArray? {
        return try {
            val uri = Uri.parse(uriString)
            platformContext.context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
}
