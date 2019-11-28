package org.nypl.simplified.viewer.epub.readium1.toc

import org.nypl.simplified.books.api.Bookmark
import java.io.Serializable

sealed class ReaderTOCSelection : Serializable {

  data class ReaderSelectedTOCElement(
    val readerTOCElement: ReaderTOCElement
  ) : ReaderTOCSelection()

  data class ReaderSelectedBookmark(
    val readerBookmark: Bookmark
  ) : ReaderTOCSelection()
}
