package fintrack.proyecto4.database

class DatabaseHelper(databaseDriverFactory: DatabaseDriverFactory) {
    val database = FinTrackDatabase(databaseDriverFactory.createDriver())
    val queries = database.finTrackDatabaseQueries
}
