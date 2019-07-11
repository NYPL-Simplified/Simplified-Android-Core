package org.nypl.simplified.app.pdf

import java.io.File
import java.io.Serializable

/**
 * Represents the parameters to pass to the [PdfReaderActivity].
 * @param documentTtile Title of the PDF
 * @param pdfFile PDF file to load
 * @param pageIndex The page to open the asset to
 */
data class PdfReaderParameters(
        val documentTile: String,
        val pdfFile: File,
        val pageIndex: Int
) : Serializable
