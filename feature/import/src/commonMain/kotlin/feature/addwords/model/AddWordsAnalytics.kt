package feature.addwords.model

import analytics.IAnalyticsTracker
import domain.word.add.model.WordOrigin

/*
 * Add-words analytics contract (doc/analytics-tracking-plan.md, "Add-words funnel"). Every event carries
 * `method` (WordOrigin.analyticsName) so one funnel covers every source:
 * import_started → import_preview_shown → import_confirmed | import_cancelled, plus import_failed.
 */

internal fun IAnalyticsTracker.logImportEvent(name: String, origin: WordOrigin, vararg params: Pair<String, Any>) =
    logEvent(name, mapOf("method" to origin.analyticsName, *params))

/** [step]: `load` (reading a file), `extract` (photo / AI request) or `commit` (saving the words). */
internal fun IAnalyticsTracker.logImportFailed(origin: WordOrigin, step: String, problem: AddWordsProblem) =
    logImportEvent("import_failed", origin, "step" to step, "error_type" to problem.name)
