package io.github.cepeterjob.taverndroid.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CharacterEntity::class,
        SessionEntity::class,
        TurnEntity::class,
        GenerationEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TavernDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun generationDao(): GenerationDao
}
