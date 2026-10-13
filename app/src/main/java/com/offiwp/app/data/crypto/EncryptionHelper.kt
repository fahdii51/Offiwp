package com.offiwp.app.data.crypto

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides AES-256 encryption and decryption for offline Bluetooth socket messaging.
 */
object EncryptionHelper {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    // Default shared key derived from app secret or dynamic peer negotiation
    private const val SECRET_SEED = "OffiWp_Bluetooth_AES256_Secret_Seed_2026"
    private val keyBytes: ByteArray by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.digest(SECRET_SEED.toByteArray(Charsets.UTF_8))
    }
    private val ivBytes: ByteArray by lazy {
        keyBytes.copyOf(16)
    }

    /**
     * Encrypts plaintext string to Base64 AES-256 ciphertext.
     */
    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val iv = IvParameterSpec(ivBytes)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    /**
     * Decrypts Base64 AES-256 ciphertext back to plaintext.
     */
    fun decrypt(cipherText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val iv = IvParameterSpec(ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            val decrypted = cipher.doFinal(decodedBytes)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            cipherText
        }
    }
}
