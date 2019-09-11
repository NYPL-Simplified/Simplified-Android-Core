package org.nypl.simplified.app.reader.toc

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
