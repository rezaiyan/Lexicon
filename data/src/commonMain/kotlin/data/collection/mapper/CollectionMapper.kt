package data.collection.mapper

import domain.collection.model.CollectionContent
import domain.collection.model.VocabularyCollection
import data.collection.remote.model.VocabularyCollection as RemoteVocabularyCollection
import data.collection.remote.model.VocabularyContentResponse

internal fun RemoteVocabularyCollection.toDomain(): VocabularyCollection {
    return VocabularyCollection(
        targetLanguage = targetLanguage,
        originLanguage = originLanguage,
        title = title,
        fileName = fileName,
        path = path
    )
}

internal fun VocabularyContentResponse.toDomain(): CollectionContent {
    return CollectionContent(
        targetLanguage = targetLanguage,
        originLanguage = originLanguage,
        fileName = fileName,
        content = content,
        wordCount = wordCount
    )
}
