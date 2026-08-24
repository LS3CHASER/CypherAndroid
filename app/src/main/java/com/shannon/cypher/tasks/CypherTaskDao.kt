package com.shannon.cypher.tasks

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update


@Dao
interface CypherTaskDao {

    @Insert
    suspend fun insert(
        task: CypherTaskEntity,
    ): Long


    @Update
    suspend fun update(
        task: CypherTaskEntity,
    )


    @Query(
        """
        SELECT *
        FROM cypher_tasks
        ORDER BY createdAtMillis ASC
        """
    )
    suspend fun getAllTasks():
            List<CypherTaskEntity>


    @Query(
        """
        SELECT *
        FROM cypher_tasks
        WHERE id = :taskId
        LIMIT 1
        """
    )
    suspend fun getById(
        taskId: Long,
    ): CypherTaskEntity?


    @Query(
        """
        DELETE FROM cypher_tasks
        WHERE id = :taskId
        """
    )
    suspend fun deleteById(
        taskId: Long,
    ): Int


    @Query(
        """
        DELETE FROM cypher_tasks
        WHERE completedAtMillis IS NOT NULL
        """
    )
    suspend fun clearCompleted()
}