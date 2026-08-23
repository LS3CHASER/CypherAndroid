package com.shannon.cypher.memory

import android.content.Context


class CypherMemoryRepository(
    context: Context,
) {

    private val database =
        CypherMemoryDatabase
            .getInstance(
                context
            )


    private val memoryDao =
        database.memoryDao()


    private val crypto =
        CypherMemoryCrypto()


    suspend fun remember(
        key: String,
        value: String,
    ) {

        val cleanKey =
            normaliseKey(
                key
            )


        val encrypted =
            crypto.encrypt(
                value.trim()
            )


        val existing =
            memoryDao.getByKey(
                cleanKey
            )


        val now =
            System.currentTimeMillis()


        val memory =
            CypherMemoryEntity(
                key =
                    cleanKey,

                encryptedValue =
                    encrypted.encryptedValue,

                initializationVector =
                    encrypted.initializationVector,

                createdAtMillis =
                    existing
                        ?.createdAtMillis
                        ?: now,

                updatedAtMillis =
                    now,
            )


        memoryDao.upsert(
            memory
        )
    }


    suspend fun recall(
        key: String,
    ): String? {

        val cleanKey =
            normaliseKey(
                key
            )


        val memory =
            memoryDao.getByKey(
                cleanKey
            )
                ?: return null


        return try {

            crypto.decrypt(
                encryptedValue =
                    memory.encryptedValue,

                initializationVector =
                    memory.initializationVector,
            )

        } catch (
            _: Exception
        ) {

            null
        }
    }


    suspend fun forget(
        key: String,
    ): Boolean {

        val cleanKey =
            normaliseKey(
                key
            )


        return memoryDao.deleteByKey(
            cleanKey
        ) > 0
    }


    suspend fun getAll():
            Map<String, String> {

        val memories =
            memoryDao.getAll()


        val result =
            linkedMapOf<String, String>()


        for (
        memory in memories
        ) {

            try {

                val decrypted =
                    crypto.decrypt(
                        encryptedValue =
                            memory.encryptedValue,

                        initializationVector =
                            memory.initializationVector,
                    )


                result[
                    memory.key
                ] = decrypted

            } catch (
                _: Exception
            ) {

                // Skip corrupted or unreadable entries.
            }
        }


        return result
    }


    suspend fun clearAll() {

        memoryDao.deleteAll()
    }


    private fun normaliseKey(
        key: String,
    ): String {

        return key
            .lowercase()
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
    }
}