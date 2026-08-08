package com.example.docvault.ui.detail

import com.example.docvault.domain.model.Document

/**
 * UI State for the Document Detail screen.
 *
 * @property document The document being viewed, or null if it's still loading or not found.
 * @property isLoading Whether the document data is currently being fetched.
 * @property error An optional error message to display if document loading fails.
 */
data class DocumentDetailUiState(
    val document: Document? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
