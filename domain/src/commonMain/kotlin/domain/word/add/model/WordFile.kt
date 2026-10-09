package domain.word.add.model

/** Raw bytes of a picked vocabulary file. [name] is informational only; content decides the format. */
class WordFile(val name: String?, val bytes: ByteArray)
