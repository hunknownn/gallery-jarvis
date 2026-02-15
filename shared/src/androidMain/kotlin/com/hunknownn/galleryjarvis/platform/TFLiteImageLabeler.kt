package com.hunknownn.galleryjarvis.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hunknownn.galleryjarvis.Constants
import com.hunknownn.galleryjarvis.labeling.LabelCategory
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite 기반 이미지 분류기.
 *
 * MobileNetV3 Small classification 모델을 사용하여
 * 이미지를 1001개 ImageNet 클래스로 분류하고 한국어 카테고리를 반환한다.
 *
 * - 입력: 224x224x3 RGB 이미지 (픽셀값 [0, 1] 정규화)
 * - 출력: 1001 클래스 확률 배열 → argmax → [LabelCategory.classify]
 */
actual class ImageLabeler(
    private val platformContext: PlatformContext
) {
    private val interpreter: Interpreter? by lazy {
        try {
            val model = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            Interpreter(model, options)
        } catch (e: Exception) {
            android.util.Log.e("ImageLabeler", "모델 로드 실패", e)
            null
        }
    }

    /** 분류 모델의 출력 클래스 수 (ImageNet-1000 + background) */
    private val numClasses: Int by lazy {
        val outputShape = interpreter?.getOutputTensor(0)?.shape()
        outputShape?.get(1) ?: 0
    }

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
     * 이미지를 분류하여 한국어 카테고리 라벨을 반환한다.
     *
     * @param imageData JPEG/PNG 등 이미지 바이너리 데이터
     * @return 한국어 카테고리 라벨 (예: "동물", "음식"). 분류 실패 시 null.
     */
    actual fun classifyImage(imageData: ByteArray): String? {
        val model = interpreter ?: return null
        if (numClasses == 0) return null

        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: return null
        val resized = Bitmap.createScaledBitmap(
            bitmap, Constants.IMAGE_SIZE, Constants.IMAGE_SIZE, true
        )

        val input = preprocessBitmap(resized)
        val output = Array(1) { FloatArray(numClasses) }
        model.run(input, output)

        if (resized != bitmap) resized.recycle()
        bitmap.recycle()

        val probabilities = output[0]
        var maxIndex = 0
        var maxProb = probabilities[0]
        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIndex = i
            }
        }

        return LabelCategory.classify(maxIndex, maxProb)
    }

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
        interpreter?.close()
    }

    companion object {
        private const val MODEL_FILE_NAME = "mobilenet_v3_small_cls.tflite"
    }
}
