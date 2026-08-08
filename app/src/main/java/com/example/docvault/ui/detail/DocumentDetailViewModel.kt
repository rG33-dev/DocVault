package com.example.docvault.ui.detail

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docvault.data.repository.DocumentRepository
import com.example.docvault.data.repository.FileRepository
import com.example.docvault.domain.model.Document
import com.example.docvault.domain.model.DocumentCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import javax.inject.Inject

/**
 * ViewModel for the Document Detail screen.
 * 
 * Handles document metadata updates, deletion, processing, and sharing.
 */
@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val fileRepository: FileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val docId: Long = checkNotNull(savedStateHandle["docId"])

    val uiState: StateFlow<DocumentDetailUiState> = repository.getDocumentById(docId)
        .map { doc ->
            if (doc != null) {
                DocumentDetailUiState(document = doc, isLoading = false)
            } else {
                DocumentDetailUiState(error = "Document not found", isLoading = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DocumentDetailUiState(isLoading = true)
        )

    fun deleteDocument() {
        val doc = uiState.value.document ?: return
        viewModelScope.launch {
            repository.deleteDocument(doc)
        }
    }

    fun updateMetadata(title: String, category: DocumentCategory) {
        viewModelScope.launch {
            repository.updateDocumentMetadata(docId, title, category)
        }
    }

    fun processDocument(
        compress: Boolean, 
        toPdf: Boolean, 
        quality: Int = 70, 
        targetSizeKb: Int? = null
    ) {
        val doc = uiState.value.document ?: return
        viewModelScope.launch {
            repository.processDocument(doc, compress, toPdf, quality, targetSizeKb)
        }
    }

    fun getDecryptedStream(filePath: String): InputStream {
        return fileRepository.getEncryptedFile(filePath)
    }

    /**
     * Decrypts the document into a temporary file in the cache directory 
     * to allow external applications to access it via FileProvider.
     */
    fun getShareUri(context: Context): Uri? {
        val doc = uiState.value.document ?: return null
        return try {
            val cacheDir = File(context.cacheDir, "shared_documents").apply { if (!exists()) mkdirs() }
            val extension = if (doc.fileType == "application/pdf") ".pdf" else ".jpg"
            val tempFile = File(cacheDir, "${doc.title.replace(" ", "_")}$extension")
            
            fileRepository.getEncryptedFile(doc.filePath).use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            FileProvider.getUriForFile(context, "com.example.docvault.fileprovider", tempFile)
        } catch (e: Exception) {
            null
        }
    }
}
