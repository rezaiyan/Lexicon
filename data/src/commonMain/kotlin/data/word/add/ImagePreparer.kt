package data.word.add

import core.common.Try
import core.error.DomainError
import domain.word.add.service.IImagePreparer
import domain.word.add.service.ImageLimits
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.normalizeForUpload

/** Re-encodes with progressively smaller size/quality until the photo fits the upload limit. */
class ImagePreparer(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val encode: (ByteArray, Int, Float, Int) -> ByteArray? = { bytes, edge, quality, quarterTurns ->
        bytes.normalizeForUpload(edge, quality, quarterTurns)
    },
) : IImagePreparer {

    override suspend fun prepare(image: ByteArray, quarterTurns: Int): Try<ByteArray> = withContext(dispatcher) {
        if (image.size < ImageLimits.MIN_UPLOAD_BYTES) {
            return@withContext Try.failure(DomainError.AddWords.ImageUnreadable)
        }
        for ((edge, quality) in Attempts) {
            val encoded = encode(image, edge, quality, quarterTurns)
                ?: return@withContext Try.failure(DomainError.AddWords.ImageUnreadable)
            if (encoded.size <= ImageLimits.MAX_UPLOAD_BYTES) return@withContext Try.success(encoded)
        }
        Try.failure(DomainError.AddWords.ImageTooLarge(ImageLimits.MAX_UPLOAD_BYTES))
    }

    private companion object {
        val Attempts = listOf(
            ImageLimits.MAX_EDGE_PX to 0.85f,
            ImageLimits.MAX_EDGE_PX to 0.7f,
            1_600 to 0.7f,
            1_280 to 0.6f,
        )
    }
}
