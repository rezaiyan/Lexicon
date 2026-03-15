package tts

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AndroidModelFileManager(
    private val context: Context
) : IModelFileManager {

    private val ttsModelsDir: File
        get() = File(context.filesDir, "tts_models")

    private fun languageDir(languageCode: String): File =
        File(ttsModelsDir, languageCode)

    override suspend fun isModelPresent(languageCode: String): Boolean {
        val dir = languageDir(languageCode)
        if (!dir.exists()) return false
        val hasOnnx = dir.walkTopDown().any { it.extension == "onnx" }
        val hasTokens = dir.walkTopDown().any { it.name == "tokens.txt" }
        val hasEspeak = dir.walkTopDown().any { it.name == "espeak-ng-data" && it.isDirectory }
        Log.d(TAG, "isModelPresent($languageCode): onnx=$hasOnnx, tokens=$hasTokens, espeak=$hasEspeak")
        return hasOnnx && hasTokens && hasEspeak
    }

    override suspend fun downloadAndExtractModel(
        archiveUrl: String,
        languageCode: String,
        extractedDirName: String
    ): Flow<Float> = flow {
        val dir = languageDir(languageCode)
        dir.mkdirs()

        val tempFile = File(dir, "model_archive.tar.bz2")

        Log.d(TAG, "Downloading model from: $archiveUrl")

        // Download archive
        val connection = URL(archiveUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 120_000
        connection.instanceFollowRedirects = true

        // Handle GitHub redirects
        val responseCode = connection.responseCode
        val finalConnection = if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
            responseCode == 307
        ) {
            val redirectUrl = connection.getHeaderField("Location")
            Log.d(TAG, "Redirected to: $redirectUrl")
            connection.disconnect()
            val redirect = URL(redirectUrl).openConnection() as HttpURLConnection
            redirect.connectTimeout = 30_000
            redirect.readTimeout = 120_000
            redirect
        } else {
            connection
        }

        val totalBytes = finalConnection.contentLengthLong
        var downloadedBytes = 0L

        Log.d(TAG, "Download size: ${totalBytes / (1024 * 1024)} MB")

        finalConnection.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        emit(downloadedBytes.toFloat() / totalBytes.toFloat() * 0.8f)
                    }
                }
            }
        }

        Log.d(TAG, "Download complete: ${tempFile.length()} bytes, extracting...")

        // Extract tar.bz2
        emit(0.85f)
        extractTarBz2(tempFile, dir, extractedDirName)

        // Clean up archive
        tempFile.delete()

        Log.d(TAG, "Extraction complete. Files in ${dir.absolutePath}:")
        dir.walkTopDown().take(20).forEach { Log.d(TAG, "  ${it.relativeTo(dir)}") }

        emit(1f)
    }.flowOn(Dispatchers.IO)

    private fun extractTarBz2(archiveFile: File, targetDir: File, extractedDirName: String) {
        BufferedInputStream(archiveFile.inputStream()).use { bis ->
            BZip2CompressorInputStream(bis).use { bzis ->
                TarArchiveInputStream(bzis).use { tais ->
                    var entry = tais.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            // Strip the top-level directory from the path
                            // e.g., "vits-piper-en_US-kristin-medium/tokens.txt" -> "tokens.txt"
                            val entryPath = entry.name
                            val relativePath = if (entryPath.startsWith("$extractedDirName/")) {
                                entryPath.removePrefix("$extractedDirName/")
                            } else {
                                entryPath
                            }

                            val outFile = File(targetDir, relativePath)
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                tais.copyTo(fos)
                            }
                        }
                        entry = tais.nextEntry
                    }
                }
            }
        }
    }

    override fun getModelFilePath(languageCode: String): String {
        val dir = languageDir(languageCode)
        val modelFile = dir.walkTopDown().find { it.extension == "onnx" }
        Log.d(TAG, "getModelFilePath($languageCode): ${modelFile?.absolutePath ?: "NOT FOUND"}")
        return modelFile?.absolutePath ?: ""
    }

    override fun getTokensFilePath(languageCode: String): String {
        val dir = languageDir(languageCode)
        val tokensFile = dir.walkTopDown().find { it.name == "tokens.txt" }
        Log.d(TAG, "getTokensFilePath($languageCode): ${tokensFile?.absolutePath ?: "NOT FOUND"}")
        return tokensFile?.absolutePath ?: ""
    }

    override fun getDataDir(languageCode: String): String {
        val dir = languageDir(languageCode)
        val espeakDir = dir.walkTopDown().find { it.name == "espeak-ng-data" && it.isDirectory }
        Log.d(TAG, "getDataDir($languageCode): ${espeakDir?.absolutePath ?: "NOT FOUND"}")
        return espeakDir?.absolutePath ?: ""
    }

    override suspend fun deleteModelFiles(languageCode: String) {
        languageDir(languageCode).deleteRecursively()
    }

    override suspend fun getModelDirectorySize(languageCode: String): Long {
        val dir = languageDir(languageCode)
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    companion object {
        private const val TAG = "TtsModelManager"
    }
}
