package tts

import bz2lib.BZ2_bzDecompress
import bz2lib.BZ2_bzDecompressEnd
import bz2lib.BZ2_bzDecompressInit
import bz2lib.BZ_OK
import bz2lib.BZ_STREAM_END
import bz2lib.bz_stream
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToFile

class IosModelFileManager : IModelFileManager {

    private val fileManager = NSFileManager.defaultManager
    private val ttsModelsDir: String
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(
                directory = 9u, // NSDocumentDirectory
                domainMask = 1u, // NSUserDomainMask
                expandTilde = true
            )
            val docs = (paths.firstOrNull() as? String) ?: ""
            return "$docs/tts_models"
        }

    private fun languageDir(languageCode: String): String = "$ttsModelsDir/$languageCode"

    override suspend fun isModelPresent(languageCode: String): Boolean {
        val dir = languageDir(languageCode)
        if (!fileManager.fileExistsAtPath(dir)) return false
        val hasOnnx = findFile(dir, "onnx") != null
        val hasTokens = findFileByName(dir, "tokens.txt") != null
        val hasEspeak = findDirectoryByName(dir, "espeak-ng-data") != null
        NSLog("IosModelFileManager: isModelPresent($languageCode): onnx=$hasOnnx tokens=$hasTokens espeak=$hasEspeak")
        return hasOnnx && hasTokens && hasEspeak
    }

    override suspend fun downloadAndExtractModel(
        archiveUrl: String,
        languageCode: String,
        extractedDirName: String
    ): Flow<Float> = flow {
        val dir = languageDir(languageCode)
        fileManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)

        val tempFile = "$dir/model_archive.tar.bz2"
        NSLog("IosModelFileManager: Downloading model from: $archiveUrl")

        emit(0.05f)

        downloadFile(archiveUrl, tempFile)

        emit(0.8f)
        NSLog("IosModelFileManager: Download complete, extracting...")

        extractTarBz2(tempFile, dir, extractedDirName)

        fileManager.removeItemAtPath(tempFile, error = null)

        NSLog("IosModelFileManager: Extraction complete")
        emit(1f)
    }.flowOn(Dispatchers.IO)

    private fun downloadFile(urlString: String, destPath: String) {
        val url = NSURL.URLWithString(urlString)
            ?: throw Exception("Invalid URL: $urlString")

        // Synchronous download — called from Dispatchers.IO context
        val data = NSData.dataWithContentsOfURL(url)
            ?: throw Exception("Download failed for: $urlString")

        NSLog("IosModelFileManager: Downloaded ${data.length} bytes")
        data.writeToFile(destPath, atomically = true)
    }

    private fun extractTarBz2(archivePath: String, targetDir: String, extractedDirName: String) {
        val compressedData = NSData.dataWithContentsOfFile(archivePath) ?: run {
            NSLog("IosModelFileManager: Could not read archive file")
            return
        }

        NSLog("IosModelFileManager: Archive size: ${compressedData.length} bytes, decompressing bz2...")

        val decompressed = decompressBz2(compressedData)
        NSLog("IosModelFileManager: Decompressed to ${decompressed.size} bytes, parsing tar...")

        parseTar(decompressed, targetDir, extractedDirName)
    }

    private fun decompressBz2(data: NSData): ByteArray {
        val inputSize = data.length.toInt()
        var outputCapacity = inputSize * 6
        var output = ByteArray(outputCapacity)

        val inputBytes = data.bytes ?: return ByteArray(0)

        memScoped {
            val stream = alloc<bz_stream>()
            stream.next_in = inputBytes.reinterpret<ByteVar>()
            stream.avail_in = inputSize.convert()

            val initResult = BZ2_bzDecompressInit(stream.ptr, 0, 0)
            if (initResult != BZ_OK) {
                NSLog("IosModelFileManager: BZ2_bzDecompressInit failed: $initResult")
                return ByteArray(0)
            }

            var totalOutput = 0

            while (true) {
                if (totalOutput >= outputCapacity - 65536) {
                    outputCapacity *= 2
                    output = output.copyOf(outputCapacity)
                }

                output.usePinned { pinned ->
                    stream.next_out = (pinned.addressOf(totalOutput) as CPointer<ByteVar>)
                    stream.avail_out = (outputCapacity - totalOutput).convert()

                    val result = BZ2_bzDecompress(stream.ptr)

                    val produced = (outputCapacity - totalOutput).toUInt() - stream.avail_out
                    totalOutput += produced.toInt()

                    when (result) {
                        BZ_STREAM_END -> {
                            BZ2_bzDecompressEnd(stream.ptr)
                            return output.copyOf(totalOutput)
                        }
                        BZ_OK -> { /* continue */ }
                        else -> {
                            NSLog("IosModelFileManager: BZ2_bzDecompress error: $result")
                            BZ2_bzDecompressEnd(stream.ptr)
                            return ByteArray(0)
                        }
                    }
                }
            }
        }
    }

    private fun parseTar(tarData: ByteArray, targetDir: String, extractedDirName: String) {
        var offset = 0
        var fileCount = 0

        while (offset + 512 <= tarData.size) {
            val header = tarData.copyOfRange(offset, offset + 512)
            offset += 512

            // End-of-archive: two zero blocks
            if (header.all { it == 0.toByte() }) break

            // Parse filename (offset 0, 100 bytes)
            val rawName = header.copyOfRange(0, 100)
                .takeWhile { it != 0.toByte() }
                .toByteArray()
                .decodeToString()

            // Parse size (offset 124, 12 bytes, octal ASCII)
            val sizeStr = header.copyOfRange(124, 136)
                .takeWhile { it != 0.toByte() && it != 0x20.toByte() }
                .toByteArray()
                .decodeToString()
                .trim()
            val fileSize = if (sizeStr.isNotEmpty()) sizeStr.toLongOrNull(8) ?: 0L else 0L

            // Parse type flag (offset 156, 1 byte)
            val typeFlag = header[156]

            // Strip top-level directory
            val relativePath = if (rawName.startsWith("$extractedDirName/")) {
                rawName.removePrefix("$extractedDirName/")
            } else {
                rawName
            }

            if (relativePath.isEmpty() || relativePath == "/") {
                val dataBlocks = ((fileSize + 511) / 512).toInt()
                offset += dataBlocks * 512
                continue
            }

            val fullPath = "$targetDir/$relativePath"

            when {
                typeFlag == 53.toByte() || rawName.endsWith("/") -> {
                    // Directory
                    fileManager.createDirectoryAtPath(fullPath, withIntermediateDirectories = true, attributes = null, error = null)
                }
                typeFlag == 48.toByte() || typeFlag == 0.toByte() -> {
                    // Regular file
                    if (fileSize > 0 && offset + fileSize.toInt() <= tarData.size) {
                        val fileData = tarData.copyOfRange(offset, offset + fileSize.toInt())

                        val parentPath = fullPath.substringBeforeLast("/")
                        fileManager.createDirectoryAtPath(parentPath, withIntermediateDirectories = true, attributes = null, error = null)

                        fileData.usePinned { pinned ->
                            val nsData = NSData.create(bytes = pinned.addressOf(0), length = fileData.size.convert())
                            nsData.writeToFile(fullPath, atomically = true)
                        }
                        fileCount++
                    }
                }
            }

            val dataBlocks = ((fileSize + 511) / 512).toInt()
            offset += dataBlocks * 512
        }

        NSLog("IosModelFileManager: Extracted $fileCount files")
    }

    override fun getModelFilePath(languageCode: String): String {
        val dir = languageDir(languageCode)
        val path = findFile(dir, "onnx") ?: ""
        NSLog("IosModelFileManager: getModelFilePath($languageCode): ${path.ifEmpty { "NOT FOUND" }}")
        return path
    }

    override fun getTokensFilePath(languageCode: String): String {
        val dir = languageDir(languageCode)
        val path = findFileByName(dir, "tokens.txt") ?: ""
        NSLog("IosModelFileManager: getTokensFilePath($languageCode): ${path.ifEmpty { "NOT FOUND" }}")
        return path
    }

    override fun getDataDir(languageCode: String): String {
        val dir = languageDir(languageCode)
        val path = findDirectoryByName(dir, "espeak-ng-data") ?: ""
        NSLog("IosModelFileManager: getDataDir($languageCode): ${path.ifEmpty { "NOT FOUND" }}")
        return path
    }

    override suspend fun deleteModelFiles(languageCode: String) {
        fileManager.removeItemAtPath(languageDir(languageCode), error = null)
    }

    override suspend fun getModelDirectorySize(languageCode: String): Long {
        val dir = languageDir(languageCode)
        if (!fileManager.fileExistsAtPath(dir)) return 0L
        return calculateDirectorySize(dir)
    }

    private fun calculateDirectorySize(path: String): Long {
        val contents = fileManager.contentsOfDirectoryAtPath(path, error = null) ?: return 0L
        var totalSize = 0L
        for (item in contents) {
            val name = item as? String ?: continue
            val fullPath = "$path/$name"
            if (isDirectory(fullPath)) {
                totalSize += calculateDirectorySize(fullPath)
            } else {
                val attrs = fileManager.attributesOfItemAtPath(fullPath, error = null)
                val fileSize = (attrs?.get(platform.Foundation.NSFileSize) as? Number)?.toLong() ?: 0L
                totalSize += fileSize
            }
        }
        return totalSize
    }

    private fun findFile(directory: String, extension: String): String? {
        val contents = fileManager.contentsOfDirectoryAtPath(directory, error = null) ?: return null
        for (item in contents) {
            val name = item as? String ?: continue
            val fullPath = "$directory/$name"
            if (isDirectory(fullPath)) {
                findFile(fullPath, extension)?.let { return it }
            } else if (name.endsWith(".$extension")) {
                return fullPath
            }
        }
        return null
    }

    private fun findFileByName(directory: String, fileName: String): String? {
        val contents = fileManager.contentsOfDirectoryAtPath(directory, error = null) ?: return null
        for (item in contents) {
            val name = item as? String ?: continue
            val fullPath = "$directory/$name"
            if (isDirectory(fullPath)) {
                findFileByName(fullPath, fileName)?.let { return it }
            } else if (name == fileName) {
                return fullPath
            }
        }
        return null
    }

    private fun findDirectoryByName(directory: String, dirName: String): String? {
        val contents = fileManager.contentsOfDirectoryAtPath(directory, error = null) ?: return null
        for (item in contents) {
            val name = item as? String ?: continue
            val fullPath = "$directory/$name"
            if (isDirectory(fullPath)) {
                if (name == dirName) return fullPath
                findDirectoryByName(fullPath, dirName)?.let { return it }
            }
        }
        return null
    }

    private fun isDirectory(path: String): Boolean {
        // Use a simple heuristic: if the path has contents, it's a directory
        val contents = fileManager.contentsOfDirectoryAtPath(path, error = null)
        return contents != null
    }
}
