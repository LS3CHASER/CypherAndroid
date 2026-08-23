package com.shannon.cypher.memory

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase


@Database(
    entities = [
        CypherMemoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CypherMemoryDatabase :
    RoomDatabase() {

    abstract fun memoryDao():
            CypherMemoryDao


    companion object {

        @Volatile
        private var INSTANCE:
                CypherMemoryDatabase? = null


        fun getInstance(
            context: Context,
        ): CypherMemoryDatabase {

            return INSTANCE
                ?: synchronized(
                    this
                ) {

                    INSTANCE
                        ?: Room
                            .databaseBuilder(
                                context.applicationContext,
                                CypherMemoryDatabase::class.java,
                                "cypher_memory.db",
                            )
                            .build()
                            .also {
                                INSTANCE = it
                            }
                }
        }
    }
}