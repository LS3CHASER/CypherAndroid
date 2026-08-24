package com.shannon.cypher.tasks

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase


@Database(
    entities = [
        CypherTaskEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CypherTaskDatabase :
    RoomDatabase() {

    abstract fun taskDao():
            CypherTaskDao


    companion object {

        @Volatile
        private var INSTANCE:
                CypherTaskDatabase? = null


        fun getInstance(
            context: Context,
        ): CypherTaskDatabase {

            return INSTANCE
                ?: synchronized(
                    this
                ) {

                    INSTANCE
                        ?: Room
                            .databaseBuilder(
                                context.applicationContext,
                                CypherTaskDatabase::class.java,
                                "cypher_tasks.db",
                            )
                            .build()
                            .also {
                                INSTANCE = it
                            }
                }
        }
    }
}