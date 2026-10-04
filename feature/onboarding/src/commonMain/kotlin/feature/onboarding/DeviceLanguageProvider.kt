package feature.onboarding

/** ISO 639-1 code of the phone's language (e.g. "en"), or null when unknown. */
fun interface DeviceLanguageProvider {
    fun languageCode(): String?
}
