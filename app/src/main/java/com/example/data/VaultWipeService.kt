package com.example.data

import android.content.Context
import com.example.crypto.KeyManager
import com.example.data.room.AegisDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.Arrays

data class WipeResult(
    val keysDeletedCount: Int,
    val filesShreddedCount: Int,
    val roomDatabaseCleared: Boolean,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * VaultWipeService executes emergency zeroization / panic wipe:
 * 1. Clears and drops local Room database tables & database files.
 * 2. Overwrites all internal storage files with zeroes and shreds them.
 * 3. Deletes all cryptographic keys stored in the Android Keystore.
 */
class VaultWipeService(private val context: Context) {
    private val secureRandom = SecureRandom()

    suspend fun executeCompleteWipe(
        onStepProgress: (step: String) -> Unit = {}
    ): WipeResult = withContext(Dispatchers.IO) {
        var filesShredded = 0
        var roomCleared = false
        var keysDeleted = 0

        try {
            // STEP 1: Clear and drop Room Database
            onStepProgress("Nullstiller lokal Room-database...")
            try {
                val db = AegisDatabase.getInstance(context)
                db.clearAllTables()
                AegisDatabase.closeAndResetInstance()
                context.deleteDatabase(AegisDatabase.DATABASE_NAME)
                // Delete auxiliary sqlite journal/wal files
                context.deleteDatabase("${AegisDatabase.DATABASE_NAME}-wal")
                context.deleteDatabase("${AegisDatabase.DATABASE_NAME}-shm")
                roomCleared = true
            } catch (e: Exception) {
                AegisDatabase.closeAndResetInstance()
                context.deleteDatabase(AegisDatabase.DATABASE_NAME)
                roomCleared = true
            }

            // STEP 2: Overwrite and remove files from internal storage
            onStepProgress("Overskriver og sletter filer fra intern lagring...")
            filesShredded += shredDirectory(context.filesDir)
            filesShredded += shredDirectory(context.cacheDir)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                filesShredded += shredDirectory(context.noBackupFilesDir)
            }

            // Clear SharedPreferences
            try {
                val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                if (prefsDir.exists()) {
                    prefsDir.listFiles()?.forEach { file ->
                        file.nameWithoutExtension.let { name ->
                            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
                        }
                    }
                    shredDirectory(prefsDir)
                }
            } catch (e: Exception) {
                // Ignore
            }

            // STEP 3: Delete cryptographic keys stored in Android Keystore
            onStepProgress("Destruerer kryptonøkler i Android Keystore...")
            keysDeleted = KeyManager.deleteAllKeys()

            onStepProgress("Wipe fullført: $keysDeleted nøkler og $filesShredded filer destruert.")
            WipeResult(
                keysDeletedCount = keysDeleted,
                filesShreddedCount = filesShredded,
                roomDatabaseCleared = roomCleared,
                success = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            WipeResult(
                keysDeletedCount = keysDeleted,
                filesShreddedCount = filesShredded,
                roomDatabaseCleared = roomCleared,
                success = false
            )
        }
    }

    /**
     * Overwrites file contents with random bytes or zeroes before deleting.
     */
    private fun shredFile(file: File) {
        try {
            if (file.exists() && file.isFile) {
                val length = file.length().toInt()
                if (length > 0) {
                    val zeroes = ByteArray(length)
                    FileOutputStream(file).use { fos ->
                        fos.write(zeroes)
                        fos.flush()
                    }
                }
                file.delete()
            }
        } catch (e: Exception) {
            file.delete()
        }
    }

    private fun shredDirectory(dir: File?): Int {
        if (dir == null || !dir.exists()) return 0
        var count = 0
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                count += shredDirectory(file)
                file.delete()
            } else {
                shredFile(file)
                count++
            }
        }
        return count
    }
}
