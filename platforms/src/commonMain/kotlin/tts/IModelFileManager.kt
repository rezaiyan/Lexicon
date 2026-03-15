package tts

import kotlinx.coroutines.flow.Flow

interface IModelFileManager {
    suspend fun isModelPresent(languageCode: String): Boolean
    suspend fun downloadAndExtractModel(archiveUrl: String, languageCode: String, extractedDirName: String): Flow<Float>
    fun getModelFilePath(languageCode: String): String
    fun getTokensFilePath(languageCode: String): String
    fun getDataDir(languageCode: String): String
    suspend fun deleteModelFiles(languageCode: String)
    suspend fun getModelDirectorySize(languageCode: String): Long
}
