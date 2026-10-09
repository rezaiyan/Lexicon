package domain.word.add.service

import core.common.Try

/**
 * Turns a camera/gallery photo into an upload-ready image: upright (EXIF applied, then turned
 * [quarterTurns] × 90° clockwise as the user rotated it), downscaled and
 * re-encoded until it fits [ImageLimits.MAX_UPLOAD_BYTES].
 * Fails with `ImageUnreadable` or `ImageTooLarge`.
 */
interface IImagePreparer {
    suspend fun prepare(image: ByteArray, quarterTurns: Int = 0): Try<ByteArray>
}

object ImageLimits {
    /** Raw bytes before base64; the server accepts 5 MB of JSON, base64 adds about a third. */
    const val MAX_UPLOAD_BYTES = 3 * 1024 * 1024
    const val MIN_UPLOAD_BYTES = 128
    /** Longest edge sent to the vision model; more pixels do not improve text recognition. */
    const val MAX_EDGE_PX = 2_048
}
