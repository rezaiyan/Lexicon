package domain.ai.model

import kotlin.jvm.JvmInline

/**
 * The learner's own words to the AI ("only the highlighted words"), at most [MAX_LENGTH]
 * characters. Untrusted: the server screens it before use and may refuse it.
 */
@JvmInline
value class AiInstruction private constructor(val text: String) {

    companion object {
        const val MAX_LENGTH = 300
        private val WHITESPACE = Regex("""\s+""")

        /** Single-spaced and trimmed; null when nothing is left. Longer text is cut to [MAX_LENGTH]. */
        fun of(raw: String): AiInstruction? =
            raw.replace(WHITESPACE, " ").trim().take(MAX_LENGTH).ifEmpty { null }?.let(::AiInstruction)
    }
}

/** Kinds of item a learner can ask the AI for. */
enum class WordType { NOUN, VERB, ADJECTIVE, PHRASE, IDIOM }

/** The tone of the vocabulary. */
enum class Register { FORMAL, EVERYDAY, SLANG }

/** What goes in each item's note; none keeps it a short hint. */
enum class NoteStyle { EXAMPLE, GRAMMAR }

/**
 * Quick picks: fixed options the server turns into its own prompt wording, so they are free and
 * never screened. [markedOnly] and [skipNames] only apply to photos.
 */
data class VocabularyFocus(
    val wordTypes: Set<WordType> = emptySet(),
    val register: Register? = null,
    val noteStyle: NoteStyle? = null,
    val markedOnly: Boolean = false,
    val skipNames: Boolean = false,
) {
    val isEmpty: Boolean
        get() = wordTypes.isEmpty() && register == null && noteStyle == null && !markedOnly && !skipNames
}

/**
 * Everything the learner asked for beyond the defaults. With an [instruction] the request is
 * "guided": it is screened server-side and costs the guided price.
 */
data class AiGuidance(
    val focus: VocabularyFocus = VocabularyFocus(),
    val instruction: AiInstruction? = null,
) {
    val isGuided: Boolean get() = instruction != null
}
