package domain.word.add.parser

/** Decodes text files: UTF-16 (LE/BE, by byte order mark) or UTF-8. Returns null for binary content. */
internal object TextDecoder {

    private const val BINARY_SAMPLE = 4_096
    private const val MAX_REPLACEMENT_RATIO = 0.05

    fun decode(bytes: ByteArray): String? {
        val text = when {
            bytes.startsWith(0xFF, 0xFE) -> utf16(bytes, from = 2, littleEndian = true)
            bytes.startsWith(0xFE, 0xFF) -> utf16(bytes, from = 2, littleEndian = false)
            else -> bytes.decodeToString()
        }
        return text.takeUnless { it.looksBinary() }
    }

    private fun ByteArray.startsWith(first: Int, second: Int) =
        size >= 2 && this[0] == first.toByte() && this[1] == second.toByte()

    private fun utf16(bytes: ByteArray, from: Int, littleEndian: Boolean): String {
        val chars = CharArray((bytes.size - from) / 2) { index ->
            val a = bytes[from + index * 2].toInt() and 0xFF
            val b = bytes[from + index * 2 + 1].toInt() and 0xFF
            (if (littleEndian) (b shl 8) or a else (a shl 8) or b).toChar()
        }
        return chars.concatToString()
    }

    private fun String.looksBinary(): Boolean {
        val sample = take(BINARY_SAMPLE)
        if (sample.isEmpty()) return false
        if ('\u0000' in sample) return true
        val replacements = sample.count { it == '�' }
        return replacements.toDouble() / sample.length > MAX_REPLACEMENT_RATIO
    }
}
