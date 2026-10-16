package com.example.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted payload envelope representing zero-knowledge data in transit.
 * The blind server/relay only sees this envelope, with zero ability to decrypt.
 */
data class EncryptedEnvelope(
    val ivBase64: String,
    val ciphertextBase64: String,
    val authTagBase64: String,
    val senderEphemeralPubKeyBase64: String
)

object CryptoEngine {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "aegis_master_identity_key"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val EC_CURVE = "secp256r1"

    private val secureRandom = SecureRandom()

    /**
     * Generates a software-backed EC P-256 KeyPair for ephemeral E2EE sessions.
     */
    fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(EC_CURVE), secureRandom)
        return kpg.generateKeyPair()
    }

    /**
     * Initializes or gets the hardware-backed Master Identity Key from AndroidKeyStore.
     */
    fun getOrCreateMasterKeyPair(): KeyPair {
        return KeyManager.getOrCreateMasterIdentityKeyPair()
    }


    /**
     * Derives a symmetric AES-256-GCM secret key using ECDH (Elliptic Curve Diffie-Hellman)
     * and SHA-256 KDF from our private key and the counterparty's public key.
     */
    fun deriveSharedKey(myPrivateKey: PrivateKey, counterpartyPublicKey: PublicKey): SecretKeySpec {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(counterpartyPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // Apply SHA-256 KDF to enforce uniform 256-bit entropy
        val sha256 = MessageDigest.getInstance("SHA-256")
        val derivedBytes = sha256.digest(sharedSecret)

        // Shred raw shared secret immediately
        shred(sharedSecret)

        return SecretKeySpec(derivedBytes, "AES")
    }

    /**
     * Encrypts plaintext string using AES-256-GCM with a fresh 96-bit random IV.
     */
    fun encrypt(plaintext: String, secretKey: SecretKeySpec, senderPublicKeyBase64: String): EncryptedEnvelope {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val encryptedOutput = cipher.doFinal(plaintextBytes)

        // Split output: Java AES/GCM appends 16-byte auth tag at the end of ciphertext
        val tagLengthBytes = GCM_TAG_LENGTH_BITS / 8
        val ciphertextLength = encryptedOutput.size - tagLengthBytes

        val ciphertext = ByteArray(ciphertextLength)
        val authTag = ByteArray(tagLengthBytes)

        System.arraycopy(encryptedOutput, 0, ciphertext, 0, ciphertextLength)
        System.arraycopy(encryptedOutput, ciphertextLength, authTag, 0, tagLengthBytes)

        // Shred plaintext bytes in RAM
        shred(plaintextBytes)

        return EncryptedEnvelope(
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            authTagBase64 = Base64.encodeToString(authTag, Base64.NO_WRAP),
            senderEphemeralPubKeyBase64 = senderPublicKeyBase64
        )
    }

    /**
     * Decrypts an encrypted envelope using AES-256-GCM.
     * Recombines ciphertext and auth tag for GCM authentication check.
     */
    fun decrypt(envelope: EncryptedEnvelope, secretKey: SecretKeySpec): String {
        val iv = Base64.decode(envelope.ivBase64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(envelope.ciphertextBase64, Base64.NO_WRAP)
        val authTag = Base64.decode(envelope.authTagBase64, Base64.NO_WRAP)

        val combined = ByteArray(ciphertext.size + authTag.size)
        System.arraycopy(ciphertext, 0, combined, 0, ciphertext.size)
        System.arraycopy(authTag, 0, combined, ciphertext.size, authTag.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(combined)
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)

        // Shred temporary buffers
        shred(combined)
        shred(decryptedBytes)

        return decryptedText
    }

    /**
     * Decodes Base64 X.509 encoded public key into a PublicKey object.
     */
    fun decodePublicKey(base64PublicKey: String): PublicKey {
        val keyBytes = Base64.decode(base64PublicKey, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(keySpec)
    }

    /**
     * Encodes a PublicKey to Base64.
     */
    fun encodePublicKey(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Generates a 60-digit Signal-like Safety Number fingerprint
     * from two public keys, formatted in 12 groups of 5 digits.
     */
    fun generateSafetyNumber(key1Base64: String, key2Base64: String): String {
        val sortedKeys = listOf(key1Base64, key2Base64).sorted()
        val combined = "${sortedKeys[0]}:${sortedKeys[1]}"
        val sha512 = MessageDigest.getInstance("SHA-512")
        val hash = sha512.digest(combined.toByteArray(Charsets.UTF_8))

        val digits = StringBuilder()
        for (i in 0 until 12) {
            val offset = (i * 4) % (hash.size - 4)
            val chunk = ByteBuffer.wrap(hash, offset, 4).int
            val positiveVal = Math.abs(chunk) % 100000
            digits.append(String.format("%05d", positiveVal))
            if (i < 11) digits.append(" ")
        }
        return digits.toString()
    }

    /**
     * Hashes a PIN using SHA-256 with a device-bound salt.
     */
    fun hashPin(pin: String, salt: String): String {
        val sha = MessageDigest.getInstance("SHA-256")
        val bytes = sha.digest("$pin::$salt".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Purges all keys stored in AndroidKeyStore for Aegis.
     * Irreversible operation used by the Kill Button / Panic Wipe.
     */
    fun purgeKeyStore(): Int {
        return KeyManager.deleteAllKeys()
    }

    /**
     * Overwrites sensitive byte buffers in memory with zeroes.
     */
    fun shred(bytes: ByteArray?) {
        if (bytes != null) {
            Arrays.fill(bytes, 0.toByte())
        }
    }
}
