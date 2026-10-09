package fakes

import core.common.Try
import domain.word.add.service.IImagePreparer

class FakeImagePreparer : IImagePreparer {
    /** null = pass the image through unchanged. */
    var result: Try<ByteArray>? = null

    override suspend fun prepare(image: ByteArray): Try<ByteArray> = result ?: Try.success(image)
}
