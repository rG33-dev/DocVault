package com.example.docvault.ui.detail

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
import java.io.InputStream
import javax.inject.Inject

/**
 * ViewModel for the Document Detail screen.
 *
 * It manages the presentation of a single document's metadata and content.
 * Provides functionality for editing metadata, deleting, and performing 
 * processing tasks like aggressive compression and PDF conversion.
 *
 * @property repository The [DocumentRepository] for document data management.
 * @property fileRepository The [FileRepository] for accessing encrypted files.
 * @property savedStateHandle Used to retrieve the document ID from navigation arguments.
 */
@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val fileRepository: FileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val docId: Long = checkNotNull(savedStateHandle["docId"])

    /**
     * The UI state representing the current document and loading status.
     */
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

    /**
     * Updates the metadata (title and category) of the current document.
     *
     * @param title The new title for the document.
     * @param category The new category for the document.
     */
    fun updateMetadata(title: String, category: DocumentCategory) {
        viewModelScope.launch {
            repository.updateDocumentMetadata(docId, title, category)
        }
    }

    /**
     * Deletes the current document from the vault.
     */
    fun deleteDocument() {
        val doc = uiState.value.document ?: return
        viewModelScope.launch {
            repository.deleteDocument(doc)
        }
    }

    /**
     * Processes the current document with optional compression and PDF conversion.
     *
     * @param compress Whether to compress the document.
     * @param toPdf Whether to convert the document to PDF.
     * @param quality The quality level for compression (0-100).
     * @param targetSizeKb Optional target size in KB for aggressive compression (e.g., 39 for < 40KB).
     */
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

    /**
     * Returns an [InputStream] for the decrypted content of the file.
     *
     * @param filePath The absolute path of the encrypted file.
     * @return Decrypted [InputStream].
     */
    fun getDecryptedStream(filePath: String): InputStream {
        return fileRepository.getEncryptedFile(filePath)
    }
}
