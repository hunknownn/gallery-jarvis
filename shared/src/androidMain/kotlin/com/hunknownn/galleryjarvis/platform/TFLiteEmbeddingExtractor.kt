package com.hunknownn.galleryjarvis.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hunknownn.galleryjarvis.Constants
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite 기반 이미지 임베딩 추출기.
 *
 * MobileNetV3 Small feature_vector 모델을 사용하여
 * 이미지를 1024차원 임베딩 벡터로 변환한다.
 *
 * - 입력: 224x224x3 RGB 이미지 (픽셀값 [0, 1] 정규화)
 * - 출력: 1024차원 FloatArray
 */
actual class EmbeddingExtractor(
    private val platformContext: PlatformContext
) {
    private val interpreter: Interpreter by lazy {
        val model = loadModelFile()
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        Interpreter(model, options)
    }

    /**
     * assets에서 tflite 모델 파일을 MappedByteBuffer로 로드한다.
     */
    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = platformContext.context.assets.openFd(MODEL_FILE_NAME)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
    }

    /**
     * 이미지 바이트에서 임베딩 벡터를 추출한다.
     *
     * @param imageData JPEG/PNG 등 이미지 바이너리 데이터
     * @return 1024차원 임베딩 벡터
     */
    actual fun extractEmbedding(imageData: ByteArray): FloatArray {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: return FloatArray(Constants.EMBEDDING_DIM)
        val resized = Bitmap.createScaledBitmap(bitmap, Constants.IMAGE_SIZE, Constants.IMAGE_SIZE, true)

        val input = preprocessBitmap(resized)
        val output = Array(1) { FloatArray(Constants.EMBEDDING_DIM) }
        interpreter.run(input, output)

        // 원본 비트맵과 다른 경우에만 recycle
        if (resized != bitmap) resized.recycle()
        bitmap.recycle()

        return output[0]
    }

    /**
     * Bitmap을 모델 입력 형식의 ByteBuffer로 변환한다.
     * 픽셀값을 [0, 1] 범위로 정규화하여 RGB 순서로 배치.
     */
    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val inputSize = Constants.IMAGE_SIZE
        val buffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            buffer.putFloat((pixel and 0xFF) / 255.0f)           // B
        }
        buffer.rewind()
        return buffer
    }

    actual fun close() {
        interpreter.close()
    }

    companion object {
        private const val MODEL_FILE_NAME = "mobilenet_v3_small.tflite"
    }
}
