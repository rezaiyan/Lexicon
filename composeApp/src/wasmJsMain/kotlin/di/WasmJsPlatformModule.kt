package di

import data.core.database.AppDatabase
import org.koin.dsl.module

fun wasmJsPlatformModule() = module {
    // Web does not use Room database - all data comes from the backend API
    // Platform-specific bindings for web are handled by expect/actual in each module
}
