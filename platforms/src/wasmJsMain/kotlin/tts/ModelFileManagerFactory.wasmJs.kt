package tts

actual fun createModelFileManager(): IModelFileManager {
    return WasmJsModelFileManager()
}
