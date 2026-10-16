package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CryptoEngine
import com.example.crypto.KeyManager
import com.example.data.VaultWipeService
import com.example.data.room.AegisDatabase
import com.example.data.room.EncryptedMessageEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AegisChat", appName)
  }

  @Test
  fun `crypto engine ecdh and aes gcm roundtrip test`() {
    val aliceKey = CryptoEngine.generateEphemeralKeyPair()
    val bobKey = CryptoEngine.generateEphemeralKeyPair()

    val aliceShared = CryptoEngine.deriveSharedKey(aliceKey.private, bobKey.public)
    val bobShared = CryptoEngine.deriveSharedKey(bobKey.private, aliceKey.public)

    val secretMessage = "Top Secret Zero-Knowledge Payload"
    val alicePubBase64 = CryptoEngine.encodePublicKey(aliceKey.public)

    val envelope = CryptoEngine.encrypt(secretMessage, aliceShared, alicePubBase64)
    val decrypted = CryptoEngine.decrypt(envelope, bobShared)

    assertEquals(secretMessage, decrypted)
  }

  @Test
  fun `safety number generation is consistent`() {
    val key1 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE123456"
    val key2 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE789012"

    val safetyNum1 = CryptoEngine.generateSafetyNumber(key1, key2)
    val safetyNum2 = CryptoEngine.generateSafetyNumber(key2, key1)

    assertEquals(safetyNum1, safetyNum2)
    assertTrue(safetyNum1.length >= 60)
  }

  @Test
  fun `vault wipe service clears room database internal files and keystore`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Create a dummy file in internal storage
    val testFile = File(context.filesDir, "test_secret.bin")
    testFile.writeText("sensitive data")
    assertTrue(testFile.exists())

    // 2. Insert dummy record in Room database
    val db = AegisDatabase.getInstance(context)
    db.messageDao().insertMessage(
      EncryptedMessageEntity(
        id = "msg_test_1",
        conversationId = "conv_1",
        senderPublicKey = "pub1",
        senderName = "Tester",
        isFromMe = true,
        ciphertextBase64 = "cipher123",
        ivBase64 = "iv123",
        authTagBase64 = "tag123",
        timestamp = System.currentTimeMillis(),
        expiresAt = null,
        ephemeralTimerSeconds = 0L
      )
    )
    val messagesBefore = db.messageDao().getMessagesForConversation("conv_1")
    assertEquals(1, messagesBefore.size)

    // 3. Execute VaultWipeService
    val wipeService = VaultWipeService(context)
    val result = wipeService.executeCompleteWipe()

    assertTrue(result.success)
    assertTrue(result.roomDatabaseCleared)
    assertFalse(testFile.exists())
  }

  @Test
  fun `key manager generates non-empty sqlcipher passphrase`() {
    val passphrase = KeyManager.getOrGenerateDatabasePassphrase()
    assertNotNull(passphrase)
    assertEquals(32, passphrase.size) // 256 bits = 32 bytes
  }

  @Test
  fun `biometric manager can check availability without crashing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val status = com.example.security.BiometricAuthManager.checkBiometricAvailability(context)
    assertNotNull(status)
  }
}
