package com.hunknownn.galleryjarvis.util

object EmbeddingSerializer {

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
