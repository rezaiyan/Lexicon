package domain.connectedapps.model

/** An AI app (Claude, ChatGPT…) the learner let into their words through the Lexicon MCP sign-in. */
data class ConnectedApp(
    val id: String,
    val name: String,
    /** Where the app really lives (verified by the server), shown next to the name it chose for itself. */
    val website: String?,
    val access: Set<ConnectedAppAccess>,
    val connectedAtMillis: Long,
)

/** What a connected app may do. Unknown server scopes are dropped. */
enum class ConnectedAppAccess(val scope: String) {
    READ_WORDS("words:read"),
    ADD_WORDS("words:write"),
    READ_PROGRESS("progress:read");

    companion object {
        fun fromScope(scope: String): ConnectedAppAccess? = entries.firstOrNull { it.scope == scope }
    }
}
