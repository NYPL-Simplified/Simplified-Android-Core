package org.nypl.simplified.app.pdf

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookID
import java.io.File
import java.io.Serializable

/**
 * Represents the parameters to pass to the [PdfReaderActivity].
 * @param documentTtile Title of the PDF
 * @param pdfFile PDF file to load
 * @param pageIndex The page to open the asset to
 */
data class PdfReaderParameters(
        val accountId: AccountID,
        val documentTile: String,
        val pdfFile: File,
        val pageIndex: Int,
        val id: BookID
) : Serializable
