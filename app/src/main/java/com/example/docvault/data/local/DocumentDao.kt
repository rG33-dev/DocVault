package com.example.docvault.data.local

import androidx.room.*
import com.example.docvault.domain.model.Document
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [Document] entity.
 *
 * Defines the database operations for managing documents in the vault.
 */
@Dao
interface DocumentDao {
    /**
     * Retrieves all documents from the database, ordered by creation date (newest first).
     *
     * @return A [Flow] of document lists.
     */
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<Document>>

    /**
     * Retrieves a specific document by its unique ID.
     *
     * @param id The ID of the document to retrieve.
     * @return The document if found, null otherwise.
     */
    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): Document?

    /**
     * Retrieves documents belonging to a specific category.
     *
     * @param category The name of the category to filter by.
     * @return A [Flow] of filtered document lists.
     */
    @Query("SELECT * FROM documents WHERE category = :category ORDER BY createdAt DESC")
    fun getDocumentsByCategory(category: String): Flow<List<Document>>

    /**
     * Inserts a new document or replaces an existing one if there is a conflict.
     *
     * @param document The document to insert.
     * @return The row ID of the inserted document.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    /**
     * Updates the metadata of an existing document.
     *
     * @param document The document with updated information.
     */
    @Update
    suspend fun updateDocument(document: Document)

    /**
     * Deletes a document from the database.
     *
     * @param document The document to delete.
     */
    @Delete
    suspend fun deleteDocument(document: Document)

    /**
     * Searches for documents whose title matches the query or contains a tag matching the query.
     *
     * @param query The search term.
     * @return A [Flow] of matching document lists.
     */
    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' OR :query IN (SELECT value FROM json_each(tags))")
    fun searchDocuments(query: String): Flow<List<Document>>
}
