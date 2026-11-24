package com.example.privacy

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Uses Android Keystore-backed AES-256-GCM encryption for sensitive local text fields
 * (such as optional personal spending notes).
 *
 * Local encryption improves defense-in-depth on-device, though no local storage mechanism
 * should be presented as absolute security against a compromised unlocked device.
 */
class KeystoreCryptoManager(
    private val keyAlias: String = "savior_local_aes_key"
) {
    private fun getOrCreateKey(): SecretKey? {
        return runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(keyAlias, null) as? SecretKey
            if (existing != null) return existing

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }.getOrNull()
    }

    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        val key = getOrCreateKey() ?: return "$PLAIN_PREFIX$plainText"
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
            ENC_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        }.getOrElse { "$PLAIN_PREFIX$plainText" }
    }

    fun decrypt(stored: String): String {
        if (stored.isBlank()) return ""
        if (stored.startsWith(PLAIN_PREFIX)) return stored.removePrefix(PLAIN_PREFIX)
        if (!stored.startsWith(ENC_PREFIX)) return stored
        val key = getOrCreateKey() ?: return ""
        return runCatching {
            val combined = Base64.decode(stored.removePrefix(ENC_PREFIX), Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH) return ""
            val iv = combined.copyOfRange(0, IV_LENGTH)
            val cipherBytes = combined.copyOfRange(IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun isHardwareKeystoreAvailable(): Boolean = getOrCreateKey() != null

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
        private const val ENC_PREFIX = "enc_v1:"
        private const val PLAIN_PREFIX = "plain_v1:"
    }
}
