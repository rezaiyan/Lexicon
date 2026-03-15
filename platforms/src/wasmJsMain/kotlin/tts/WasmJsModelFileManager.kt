package tts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class WasmJsModelFileManager : IModelFileManager {
    override suspend fun isModelPresent(languageCode: String): Boolean = false

    override suspend fun downloadAndExtractModel(
        archiveUrl: String,
        languageCode: String,
        extractedDirName: String
    ): Flow<Float> = emptyFlow()

    override fun getModelFilePath(languageCode: String): String = ""

    override fun getTokensFilePath(languageCode: String): String = ""

    override fun getDataDir(languageCode: String): String = ""

    override suspend fun deleteModelFiles(languageCode: String) {
        // No-op
    }

    override suspend fun getModelDirectorySize(languageCode: String): Long = 0L
}
