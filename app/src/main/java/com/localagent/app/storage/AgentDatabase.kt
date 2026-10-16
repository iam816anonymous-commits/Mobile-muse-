package com.localagent.app.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.localagent.app.storage.dao.EventDao
import com.localagent.app.storage.dao.SessionDao
import com.localagent.app.storage.entity.AgentEventEntity
import com.localagent.app.storage.entity.AgentSessionEntity
import java.io.File

@Database(
    entities = [AgentEventEntity::class, AgentSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AgentDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val DATABASE_NAME = "agent.db"

        @Volatile
        private var INSTANCE: AgentDatabase? = null

        fun getInstance(context: Context): AgentDatabase {
            return INSTANCE ?: synchronized(this) {
                val agentDir = File(context.filesDir, "agent")
                if (!agentDir.exists()) {
                    agentDir.mkdirs()
                }
                val dbFile = File(agentDir, DATABASE_NAME)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AgentDatabase::class.java,
                    dbFile.absolutePath
                )
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
