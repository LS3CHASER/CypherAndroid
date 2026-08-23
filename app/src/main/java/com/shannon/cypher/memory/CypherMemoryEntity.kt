package com.shannon.cypher.memory

import androidx.room3.Entity
import androidx.room3.PrimaryKey


@Entity(
    tableName = "cypher_memory"
)
data class CypherMemoryEntity(

    @PrimaryKey
    val key: String,

    val encryptedValue: String,

    val initializationVector: String,

    val createdAtMillis: Long,

    val updatedAtMillis: Long,
)