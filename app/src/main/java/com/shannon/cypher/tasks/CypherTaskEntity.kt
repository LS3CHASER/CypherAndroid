package com.shannon.cypher.tasks

import androidx.room3.Entity
import androidx.room3.PrimaryKey


@Entity(
    tableName = "cypher_tasks"
)
data class CypherTaskEntity(

    @PrimaryKey(
        autoGenerate = true
    )
    val id: Long = 0,

    val title: String,

    val isCompleted: Boolean = false,

    val createdAtMillis: Long,

    val completedAtMillis: Long? = null,

    val dueAtMillis: Long? = null,
)