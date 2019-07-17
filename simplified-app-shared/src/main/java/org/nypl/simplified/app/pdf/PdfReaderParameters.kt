package org.nypl.simplified.app.pdf

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookID
import java.io.File
import java.io.Serializable

/**
 * Represents the parameters to pass to the [PdfReaderActivity].
 *
 * @property accountId Account holders ID
 * @property documentTile String title of the PDF PDF Book
 * @property pdfFile PDF file to load
 * @property id The BookID for the PDF Book
 */
data class PdfReaderParameters(
        val accountId: AccountID,
        val documentTile: String,
        val pdfFile: File,
        val id: BookID
) : Serializable
