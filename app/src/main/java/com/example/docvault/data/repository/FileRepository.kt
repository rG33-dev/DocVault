package com.example.docvault.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for low-level file operations, specifically encrypted storage.
 *
 * It uses [EncryptedFile] from the Jetpack Security library to ensure that all 
 * document content is stored securely on disk using AES256-GCM encryption.
 *
 * @property context The application context.
 * @property masterKey The master key used for file encryption and decryption.
 */
@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val masterKey: MasterKey
) {
    /**
     * The directory where all documents are stored.
     */
    private val docsDir = File(context.filesDir, "documents").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Saves an [InputStream] into an encrypted file on disk.
     *
     * @param inputStream The source data to be encrypted and saved.
     * @param fileName The name of the file to create.
     * @return The absolute path to the saved encrypted file.
     */
    fun saveEncryptedFile(inputStream: InputStream, fileName: String): String {
        val file = File(docsDir, fileName)
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return file.absolutePath
    }

    /**
     * Opens an encrypted file for reading and returns its decrypted [InputStream].
     *
     * @param filePath The absolute path to the encrypted file.
     * @return A stream providing decrypted content.
     */
    fun getEncryptedFile(filePath: String): InputStream {
        val file = File(filePath)
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return encryptedFile.openFileInput()
    }

    /**
     * Deletes a file from disk.
     *
     * @param filePath The absolute path of the file to delete.
     * @return True if deletion was successful, false otherwise.
     */
    fun deleteFile(filePath: String): Boolean {
        return File(filePath).delete()
    }
}
