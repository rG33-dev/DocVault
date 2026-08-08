package com.example.docvault.data.repository

import com.example.docvault.data.local.DocumentDao
import com.example.docvault.data.local.HistoryDao
import com.example.docvault.domain.model.Document
import com.example.docvault.domain.model.DocumentCategory
import com.example.docvault.domain.model.HistoryActionType
import com.example.docvault.domain.model.HistoryLog
import com.example.docvault.domain.util.DocumentProcessor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class that manages document data and operations.
 * 
 * It coordinates between the local database ([DocumentDao]), encrypted file storage ([FileRepository]),
 * and document processing utilities ([DocumentProcessor]). It also handles activity logging 
 * via [HistoryDao].
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao,
    private val historyDao: HistoryDao,
    private val fileRepository: FileRepository,
    private val documentProcessor: DocumentProcessor
) {
    /**
     * A [Flow] emitting the list of all documents in the vault, ordered by creation date (newest first).
     */
    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments()

    /**
     * Adds a new document to the vault by saving its file content securely and storing its metadata.
     */
    suspend fun addDocument(
        title: String,
        category: DocumentCategory,
        fileType: String,
        inputStream: InputStream,
        originalFileName: String,
        tags: List<String> = emptyList()
    ) {
        val fileName = "${System.currentTimeMillis()}_$originalFileName"
        val filePath = fileRepository.saveEncryptedFile(inputStream, fileName)
        val size = File(filePath).length()

        val document = Document(
            title = title,
            category = category,
            fileType = fileType,
            filePath = filePath,
            size = size,
            tags = tags
        )

        val id = documentDao.insertDocument(document)
        
        historyDao.insertLog(
            HistoryLog(
                actionType = HistoryActionType.UPLOADED,
                documentId = id,
                documentName = title,
                details = "Added document: $title ($category)"
            )
        )
    }

    /**
     * Updates document metadata like title and category.
     */
    suspend fun updateDocumentMetadata(documentId: Long, newTitle: String, newCategory: DocumentCategory) {
        val doc = documentDao.getDocumentById(documentId) ?: return
        val updatedDoc = doc.copy(
            title = newTitle,
            category = newCategory,
            updatedAt = System.currentTimeMillis()
        )
        documentDao.updateDocument(updatedDoc)
        
        historyDao.insertLog(
            HistoryLog(
                actionType = HistoryActionType.EDITED,
                documentId = documentId,
                documentName = newTitle,
                details = "Updated metadata for ${doc.title}"
            )
        )
    }

    /**
     * Deletes a document from the database and removes its associated encrypted file from disk.
     */
    suspend fun deleteDocument(document: Document) {
        fileRepository.deleteFile(document.filePath)
        documentDao.deleteDocument(document)
        
        historyDao.insertLog(
            HistoryLog(
                actionType = HistoryActionType.DELETED,
                documentId = null,
                documentName = document.title,
                details = "Deleted document: ${document.title}"
            )
        )
    }

    /**
     * Retrieves a single document by its unique ID.
     */
    fun getDocumentById(id: Long): Flow<Document?> {
        return flow {
            emit(documentDao.getDocumentById(id))
        }
    }

    /**
     * Performs image compression and/or PDF conversion on an existing document.
     */
    suspend fun processDocument(
        document: Document, 
        compress: Boolean, 
        toPdf: Boolean, 
        quality: Int,
        targetSizeKb: Int? = null
    ) {
        var currentStream = fileRepository.getEncryptedFile(document.filePath)
        var currentFileType = document.fileType
        var currentFileName = File(document.filePath).name
        
        if (compress && currentFileType.startsWith("image/")) {
            currentStream = documentProcessor.compressImage(currentStream, quality, targetSizeKb)
        }
        
        if (toPdf && currentFileType != "application/pdf") {
            currentStream = documentProcessor.convertToPdf(listOf(currentStream))
            currentFileType = "application/pdf"
            currentFileName = File(currentFileName).nameWithoutExtension + ".pdf"
        }
        
        val newFileName = "processed_${System.currentTimeMillis()}_$currentFileName"
        val newPath = fileRepository.saveEncryptedFile(currentStream, newFileName)
        val newSize = File(newPath).length()

        fileRepository.deleteFile(document.filePath)

        val updatedDoc = document.copy(
            filePath = newPath,
            fileType = currentFileType,
            size = newSize,
            updatedAt = System.currentTimeMillis()
        )
        documentDao.updateDocument(updatedDoc)

        val actions = mutableListOf<String>()
        if (compress) actions.add("compressed")
        if (toPdf) actions.add("converted to PDF")
        
        historyDao.insertLog(
            HistoryLog(
                actionType = if (toPdf) HistoryActionType.CONVERTED else HistoryActionType.COMPRESSED,
                documentId = document.id,
                documentName = document.title,
                details = "Processed ${document.title}: ${actions.joinToString(" and ")}"
            )
        )
    }

    /**
     * Combines multiple images into a single PDF document.
     */
    suspend fun combineImagesToPdf(documents: List<Document>, outputTitle: String) {
        val imageStreams = documents.map { fileRepository.getEncryptedFile(it.filePath) }
        val pdfStream = documentProcessor.convertToPdf(imageStreams)
        
        val fileName = "combined_${System.currentTimeMillis()}.pdf"
        val filePath = fileRepository.saveEncryptedFile(pdfStream, fileName)
        val size = File(filePath).length()

        val newDoc = Document(
            title = outputTitle,
            category = DocumentCategory.OTHER,
            fileType = "application/pdf",
            filePath = filePath,
            size = size
        )

        val id = documentDao.insertDocument(newDoc)

        historyDao.insertLog(
            HistoryLog(
                actionType = HistoryActionType.CONVERTED,
                documentId = id,
                documentName = outputTitle,
                details = "Combined ${documents.size} images into PDF: $outputTitle"
            )
        )
    }
}
