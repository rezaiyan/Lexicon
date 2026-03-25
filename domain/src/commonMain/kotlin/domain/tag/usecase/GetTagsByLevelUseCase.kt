package domain.tag.usecase

import core.common.NoParamFlowUseCase
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import kotlinx.coroutines.flow.Flow

class GetTagsByLevelUseCase(
    private val tagRepository: ITagRepository
) : NoParamFlowUseCase<Map<Int, List<Tag>>> {
    operator fun invoke(): Flow<Map<Int, List<Tag>>> = tagRepository.getTagsByLevel()
    override operator fun invoke(params: Unit) = invoke()
}
