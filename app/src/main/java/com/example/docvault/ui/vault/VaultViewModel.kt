package com.example.docvault.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docvault.data.repository.DocumentRepository
import com.example.docvault.domain.model.Document
import com.example.docvault.domain.model.DocumentCategory
import com.example.docvault.domain.util.BarcodeScanner
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

/**
 * ViewModel for the Document Vault screen.
 *
 * It manages the collection of documents, handling searching, filtering by category,
 * and adding new documents from various sources.
 *
 * @property repository The [DocumentRepository] used to interact with document data.
 * @property qrScanner Utility for scanning QR codes from imported images.
 */
@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val qrScanner: BarcodeScanner
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<DocumentCategory?>(null)
    private val _qrResult = MutableSharedFlow<String?>()
    val qrResult = _qrResult.asSharedFlow()

    /**
     * The UI state for the vault screen, reactively updated based on search and filters.
     */
    val uiState: StateFlow<VaultUiState> = combine(
        repository.allDocuments,
        _searchQuery,
        _selectedCategory
    ) { documents, query, category ->
        val filteredDocs = documents.filter { doc ->
            val matchesQuery = doc.title.contains(query, ignoreCase = true) ||
                    doc.tags.any { it.contains(query, ignoreCase = true) }
            val matchesCategory = category == null || doc.category == category
            matchesQuery && matchesCategory
        }
        VaultUiState(
            documents = filteredDocs,
            searchQuery = query,
            selectedCategory = category,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultUiState(isLoading = true)
    )

    /**
     * Updates the current search query.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Updates the currently selected category filter.
     */
    fun onCategorySelect(category: DocumentCategory?) {
        _selectedCategory.value = category
    }

    /**
     * Adds a new document to the vault.
     */
    fun addDocument(
        title: String,
        category: DocumentCategory,
        fileType: String,
        inputStream: InputStream,
        originalFileName: String
    ) {
        viewModelScope.launch {
            repository.addDocument(title, category, fileType, inputStream, originalFileName)
        }
    }

    /**
     * Scans an image for QR codes.
     */
    fun scanQrCode(image: InputImage) {
        viewModelScope.launch {
            val result = qrScanner.scanQrCode(image)
            _qrResult.emit(result)
        }
    }

    /**
     * Combines multiple documents into a single PDF.
     * 
     * @param documentIds List of document IDs to combine.
     * @param title The title for the newly created PDF.
     */
    fun combineToPdf(documentIds: List<Long>, title: String) {
        viewModelScope.launch {
            val docsToCombine = uiState.value.documents.filter { it.id in documentIds }
            if (docsToCombine.isNotEmpty()) {
                repository.combineImagesToPdf(docsToCombine, title)
            }
        }
    }

    /**
     * Deletes a document from the vault.
     */
    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            repository.deleteDocument(document)
        }
    }
}
