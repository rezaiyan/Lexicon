package data.core.database

expect class DatabaseDriverFactory {
    fun createDatabase(): AppDatabase
}

