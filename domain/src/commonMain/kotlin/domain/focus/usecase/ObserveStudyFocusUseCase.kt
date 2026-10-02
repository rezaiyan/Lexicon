package domain.focus.usecase

import core.common.NoParamFlowUseCase
import domain.focus.LearningFocusPolicy
import domain.focus.filterBy
import domain.focus.model.StudyFocusOverview
import domain.focus.repository.ILearningFocusRepository
import domain.tag.repository.ITagRepository
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** One stream for the Study screen: focus, language list, nudge, intro, scoped stats and tags. */
class ObserveStudyFocusUseCase(
    private val wordRepository: IWordRepository,
    private val tagRepository: ITagRepository,
    private val focusRepository: ILearningFocusRepository,
    private val nowMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : NoParamFlowUseCase<StudyFocusOverview> {

    operator fun invoke(): Flow<StudyFocusOverview> = combine(
        wordRepository.getAllWords(),
        tagRepository.getTags(),
        focusRepository.observePreference(),
        focusRepository.observeNudgeDismissedDay(),
        focusRepository.observeIntroAcknowledged(),
    ) { words, tags, preference, dismissedDay, introAcknowledged ->
        val now = nowMillis()
        val focus = LearningFocusPolicy.resolve(words, preference, now)
        val languages = LearningFocusPolicy.summaries(words, focus, now)
        val focusedWords = words.filterBy(focus)
        StudyFocusOverview(
            focus = focus,
            languages = languages,
            nudge = LearningFocusPolicy.nudge(languages, focus, dismissedDay, now),
            showIntro = languages.size >= 2 && !introAcknowledged,
            progressStats = LearningFocusPolicy.progressStats(focusedWords, now),
            tagStats = LearningFocusPolicy.tagStats(tags, focusedWords, focus, now),
        )
    }.distinctUntilChanged()

    override operator fun invoke(params: Unit) = invoke()
}
