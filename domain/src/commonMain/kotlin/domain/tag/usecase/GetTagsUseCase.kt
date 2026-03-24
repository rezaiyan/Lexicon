package domain.tag.usecase

import core.common.NoParamFlowUseCase
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import kotlinx.coroutines.flow.Flow

class GetTagsUseCase(
    private val tagRepository: ITagRepository
) : NoParamFlowUseCase<List<Tag>> {
    operator fun invoke(): Flow<List<Tag>> = tagRepository.getTags()
    override operator fun invoke(params: Unit) = invoke()
}
