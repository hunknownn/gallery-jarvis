package com.hunknownn.galleryjarvis.util

/**
 * 임베딩 벡터(FloatArray)를 바이너리(ByteArray)로 직렬화/역직렬화하는 유틸리티.
 *
 * Little-Endian 바이트 순서로 Float32 배열을 변환한다.
 * 파일 캐시에 임베딩을 저장하고 로드할 때 사용.
 */
object EmbeddingSerializer {

    /** FloatArray를 Little-Endian ByteArray로 직렬화한다. */
    fun serialize(embedding: FloatArray): ByteArray {
        val buffer = ByteArray(embedding.size * 4)
        for (i in embedding.indices) {
            val bits = embedding[i].toBits()
            buffer[i * 4] = (bits and 0xFF).toByte()
            buffer[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
            buffer[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
            buffer[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
        }
        return buffer
    }

    /** Little-Endian ByteArray를 FloatArray로 역직렬화한다. */
    fun deserialize(bytes: ByteArray): FloatArray {
        val result = FloatArray(bytes.size / 4)
        for (i in result.indices) {
            val bits = (bytes[i * 4].toInt() and 0xFF) or
                ((bytes[i * 4 + 1].toInt() and 0xFF) shl 8) or
                ((bytes[i * 4 + 2].toInt() and 0xFF) shl 16) or
                ((bytes[i * 4 + 3].toInt() and 0xFF) shl 24)
            result[i] = Float.fromBits(bits)
        }
        return result
    }
}
