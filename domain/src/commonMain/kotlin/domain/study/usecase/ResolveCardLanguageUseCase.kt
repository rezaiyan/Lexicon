package domain.study.usecase

import domain.word.model.Word

class ResolveCardLanguageUseCase {

    operator fun invoke(text: String, explicitCode: String, words: List<Word>): String {
        if (explicitCode.isNotBlank()) return explicitCode
        if (words.isEmpty()) return explicitCode
        val isSourceSide = words.any { it.originalWord == text }
        return words
            .map { if (isSourceSide) it.sourceLanguage.code else it.targetLanguage.code }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: explicitCode
    }
}
