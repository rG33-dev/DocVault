package com.example.docvault.domain.model

/**
 * Enumeration representing the possible categories for a document.
 * Used for classification, filtering, and organization in the vault.
 */
enum class DocumentCategory {
    /** Personal identity documents like Passports, Aadhar, Driving Licenses. */
    ID,
    /** Academic records and certificates of education. */
    MARKSHEET,
    /** Professional or achievement certificates. */
    CERTIFICATE,
    /** Purchase proofs and financial documents. */
    RECEIPT,
    /** Any other document type not covered by the specific categories. */
    OTHER
}
