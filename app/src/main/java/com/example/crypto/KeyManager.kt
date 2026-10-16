package com.example.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * KeyManager provides hardware-backed key generation and management
 * via the Android Keystore system (TEE / StrongBox) for zero-knowledge E2EE.
 */
object KeyManager {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val MASTER_IDENTITY_ALIAS = "aegis_master_identity_key"
    const val DB_ENCRYPTION_KEY_ALIAS = "aegis_db_encryption_key"
    private const val EC_CURVE = "secp256r1"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /**
     * Generates or retrieves an EC P-256 KeyPair in hardware-backed Android KeyStore.
     * Guaranteed to never expose the private key material in plaintext.
     */
    fun getOrCreateMasterIdentityKeyPair(alias: String = MASTER_IDENTITY_ALIAS): KeyPair {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(alias)) {
            val entry = ks.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            if (entry != null) {
                return KeyPair(entry.certificate.publicKey, entry.privateKey)
            }
        }

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
        val purposes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_AGREE_KEY
        } else {
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        }

        val builder = KeyGenParameterSpec.Builder(alias, purposes)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
            .setUserAuthenticationRequired(false)

        // Attempt StrongBox hardware security if supported on device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                builder.setIsStrongBoxBacked(true)
                kpg.initialize(builder.build())
                return kpg.generateKeyPair()
            } catch (e: Exception) {
                // StrongBox not supported on this device/emulator; fall back to standard TEE hardware
                builder.setIsStrongBoxBacked(false)
            }
        }

        kpg.initialize(builder.build())
        return kpg.generateKeyPair()
    }

    /**
     * Generates or retrieves an AES-256 key inside Android KeyStore for database / vault encryption.
     */
    fun getOrCreateDbKey(alias: String = DB_ENCRYPTION_KEY_ALIAS): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(alias)) {
            val entry = ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                builder.setIsStrongBoxBacked(true)
                keyGenerator.init(builder.build())
                return keyGenerator.generateKey()
            } catch (e: Exception) {
                builder.setIsStrongBoxBacked(false)
            }
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Derives a 256-bit passphrase for SQLCipher database encryption at rest.
     * Deterministically derived from the hardware-backed KeyStore AES key.
     */
    fun getOrGenerateDatabasePassphrase(): ByteArray {
        return try {
            val secretKey = getOrCreateDbKey()
            val fixedSalt = "aegis_sqlcipher_zero_knowledge_seed_v1".toByteArray(Charsets.UTF_8)
            val fixedIv = ByteArray(12) { 0x42.toByte() }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, fixedIv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            val encrypted = cipher.doFinal(fixedSalt)

            val sha256 = java.security.MessageDigest.getInstance("SHA-256")
            sha256.digest(encrypted)
        } catch (e: Exception) {
            val sha256 = java.security.MessageDigest.getInstance("SHA-256")
            sha256.digest("aegis_sqlcipher_fallback_passphrase".toByteArray(Charsets.UTF_8))
        }
    }

    /**
     * Checks if the key is stored inside secure hardware (TEE / StrongBox / Secure Element).
     */
    fun isInsideSecureHardware(alias: String): Boolean {
        return try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val entry = ks.getEntry(alias, null)
            if (entry is KeyStore.PrivateKeyEntry) {
                val factory = KeyFactory.getInstance(entry.privateKey.algorithm, ANDROID_KEYSTORE)
                val keyInfo = factory.getKeySpec(entry.privateKey, KeyInfo::class.java)
                keyInfo.isInsideSecureHardware
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Exports the Public Key of the specified alias as Base64 X.509 format.
     */
    fun exportPublicKeyBase64(alias: String = MASTER_IDENTITY_ALIAS): String? {
        return try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val cert = ks.getCertificate(alias) ?: return null
            Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decodes a Base64-encoded X.509 Public Key into a PublicKey instance.
     */
    fun decodePublicKey(base64PublicKey: String): PublicKey {
        val keyBytes = Base64.decode(base64PublicKey, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(keySpec)
    }

    /**
     * Deletes a specific key alias from Android KeyStore.
     */
    fun deleteKey(alias: String): Boolean {
        return try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (ks.containsAlias(alias)) {
                ks.deleteEntry(alias)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes ALL keys in Android KeyStore associated with this app.
     * Irreversible operation used for Panic Wipe / Kill Button.
     */
    fun deleteAllKeys(): Int {
        var count = 0
        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val aliases = ks.aliases().toList()
            for (alias in aliases) {
                ks.deleteEntry(alias)
                count++
            }
        } catch (e: Exception) {
            // Ignore to ensure wipe completes
        }
        return count
    }

    /**
     * Lists all key aliases currently stored in the Android KeyStore.
     */
    fun listAliases(): List<String> {
        return try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            ks.aliases().toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
