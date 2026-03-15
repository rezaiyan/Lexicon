package di

import data.tts.repository.TtsRepositoryImpl
import domain.tts.repository.ITtsRepository
import domain.tts.usecase.DeleteTtsModelUseCase
import domain.tts.usecase.GetTtsModelsInfoUseCase
import domain.tts.usecase.SpeakWordUseCase
import domain.tts.usecase.StopSpeakingUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import tts.IModelFileManager
import tts.ITtsEngine
import tts.createModelFileManager
import tts.createTtsEngine

fun ttsModule() = module {
    single<ITtsEngine> { createTtsEngine() }
    single<IModelFileManager> { createModelFileManager() }

    single<ITtsRepository> {
        TtsRepositoryImpl(
            ttsEngine = get(),
            modelFileManager = get(),
            performanceTracer = get(),
        )
    }

    singleOf(::SpeakWordUseCase)
    singleOf(::StopSpeakingUseCase)
    singleOf(::GetTtsModelsInfoUseCase)
    singleOf(::DeleteTtsModelUseCase)
}
