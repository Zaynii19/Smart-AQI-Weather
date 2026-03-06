package com.aqi.weather.util

import android.util.Base64
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Security {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

    // 16-byte secret key (keep this secure and don't hardcode it in the app)
    private const val SECRET_KEY = "1234567890123456"
    private val IV = ByteArray(16) // Initialization vector (should be random in production)

    private fun getSecretKey(): Key {
        return SecretKeySpec(SECRET_KEY.toByteArray(), ALGORITHM)
    }

    // Encrypt the input string
    fun encrypt(input: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), IvParameterSpec(IV))
        val encryptedBytes = cipher.doFinal(input.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT) // Convert to Base64 for storage
    }

    // Decrypt the input string
    fun decrypt(encrypted: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(IV))
        val decryptedBytes = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT))
        return String(decryptedBytes)
    }
}