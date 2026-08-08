package com.example.docvault.ui.vault

import com.example.docvault.domain.model.Document
import com.example.docvault.domain.model.DocumentCategory

/**
 * UI State for the Vault screen.
 *
 * @property documents The list of documents to display, filtered by search and category.
 * @property searchQuery The current text in the search bar.
 * @property selectedCategory The currently active category filter, or null for "All".
 * @property isLoading Whether the documents are currently being loaded from the database.
 */
data class VaultUiState(
    val documents: List<Document> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: DocumentCategory? = null,
    val isLoading: Boolean = false
)
