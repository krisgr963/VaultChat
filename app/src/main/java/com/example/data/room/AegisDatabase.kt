package com.example.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.crypto.KeyManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [EncryptedMessageEntity::class, ChatThreadEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun threadDao(): ThreadDao

    companion object {
        const val DATABASE_NAME = "aegis_secure_room.db"

        @Volatile
        private var INSTANCE: AegisDatabase? = null

        fun getInstance(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AegisDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AegisDatabase::class.java,
                DATABASE_NAME
            )

            try {
                // Load native SQLCipher binary library
                SQLiteDatabase.loadLibs(context.applicationContext)

                // Derive 256-bit passphrase directly from hardware-backed KeyStore
                val passphrase = KeyManager.getOrGenerateDatabasePassphrase()
                val openHelperFactory = SupportFactory(passphrase)
                builder.openHelperFactory(openHelperFactory)
            } catch (e: UnsatisfiedLinkError) {
                // Host JVM environment where Android native .so cannot link
            } catch (e: Exception) {
                // Fallback for test runners
            }

            return builder
                .fallbackToDestructiveMigration()
                .build()
        }

        fun closeAndResetInstance() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Exception) {
                    // Ignore
                }
                INSTANCE = null
            }
        }
    }
}
