package com.shannon.cypher.memory

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert


@Dao
interface CypherMemoryDao {

    @Upsert
    suspend fun upsert(
        memory: CypherMemoryEntity,
    )


    @Query(
        """
        SELECT *
        FROM cypher_memory
        WHERE `key` = :key
        LIMIT 1
        """
    )
    suspend fun getByKey(
        key: String,
    ): CypherMemoryEntity?


    @Query(
        """
        SELECT *
        FROM cypher_memory
        ORDER BY updatedAtMillis DESC
        """
    )
    suspend fun getAll():
            List<CypherMemoryEntity>


    @Query(
        """
        DELETE FROM cypher_memory
        WHERE `key` = :key
        """
    )
    suspend fun deleteByKey(
        key: String,
    ): Int


    @Delete
    suspend fun delete(
        memory: CypherMemoryEntity,
    )


    @Query(
        """
        DELETE FROM cypher_memory
        """
    )
    suspend fun deleteAll()
}