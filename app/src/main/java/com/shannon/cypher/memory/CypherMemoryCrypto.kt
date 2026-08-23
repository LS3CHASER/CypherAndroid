package com.shannon.cypher.memory

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


class CypherMemoryCrypto {

    data class EncryptedMemory(
        val encryptedValue: String,
        val initializationVector: String,
    )


    private val keyAlias =
        "cypher_memory_key"


    private val keyStore =
        KeyStore
            .getInstance(
                "AndroidKeyStore"
            )
            .apply {
                load(
                    null
                )
            }


    fun encrypt(
        plainText: String,
    ): EncryptedMemory {

        val secretKey =
            getOrCreateSecretKey()


        val cipher =
            Cipher.getInstance(
                "AES/GCM/NoPadding"
            )


        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
        )


        val encryptedBytes =
            cipher.doFinal(
                plainText.toByteArray(
                    Charsets.UTF_8
                )
            )


        val encryptedValue =
            Base64.encodeToString(
                encryptedBytes,
                Base64.NO_WRAP,
            )


        val initializationVector =
            Base64.encodeToString(
                cipher.iv,
                Base64.NO_WRAP,
            )


        return EncryptedMemory(
            encryptedValue =
                encryptedValue,

            initializationVector =
                initializationVector,
        )
    }


    fun decrypt(
        encryptedValue: String,
        initializationVector: String,
    ): String {

        val secretKey =
            getOrCreateSecretKey()


        val cipher =
            Cipher.getInstance(
                "AES/GCM/NoPadding"
            )


        val ivBytes =
            Base64.decode(
                initializationVector,
                Base64.NO_WRAP,
            )


        val encryptedBytes =
            Base64.decode(
                encryptedValue,
                Base64.NO_WRAP,
            )


        val parameterSpec =
            GCMParameterSpec(
                128,
                ivBytes,
            )


        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            parameterSpec,
        )


        val decryptedBytes =
            cipher.doFinal(
                encryptedBytes
            )


        return String(
            decryptedBytes,
            Charsets.UTF_8,
        )
    }


    private fun getOrCreateSecretKey():
            SecretKey {

        val existingKey =
            keyStore.getKey(
                keyAlias,
                null,
            ) as? SecretKey


        if (
            existingKey != null
        ) {

            return existingKey
        }


        val keyGenerator =
            KeyGenerator
                .getInstance(
                    KeyProperties
                        .KEY_ALGORITHM_AES,

                    "AndroidKeyStore",
                )


        val keySpec =
            KeyGenParameterSpec
                .Builder(
                    keyAlias,

                    KeyProperties
                        .PURPOSE_ENCRYPT or
                            KeyProperties
                                .PURPOSE_DECRYPT,
                )
                .setBlockModes(
                    KeyProperties
                        .BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties
                        .ENCRYPTION_PADDING_NONE
                )
                .setKeySize(
                    256
                )
                .build()


        keyGenerator.init(
            keySpec
        )


        return keyGenerator
            .generateKey()
    }
}