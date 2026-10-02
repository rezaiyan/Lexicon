package feature.profile.ui.components

private val monthNames = arrayOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

/** Formats an ISO date ("2024-03-15…") as "March 2024"; returns the input unchanged if unparseable. */
internal fun formatMemberSince(isoDate: String): String {
    val parts = isoDate.split("-")
    if (parts.size < 2) return isoDate
    val year = parts[0]
    val monthIndex = parts[1].toIntOrNull()?.minus(1) ?: return isoDate
    if (monthIndex !in monthNames.indices) return isoDate
    return "${monthNames[monthIndex]} $year"
}
