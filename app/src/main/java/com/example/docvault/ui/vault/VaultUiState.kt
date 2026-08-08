package com.example.docvault.ui.vault

import com.example.docvault.domain.model.Document
import com.example.docvault.domain.model.DocumentCategory

/**
 * UI State for the Vault screen.
 */
data class VaultUiState(
    val documents: List<Document> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: DocumentCategory? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
)
