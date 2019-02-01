package org.nypl.simplified.app.reader.toc

import org.nypl.simplified.books.reader.ReaderBookmark
import java.io.Serializable

data class ReaderTOCParameters(
  val bookmarks: List<ReaderBookmark>,
  val tocElements: List<ReaderTOCElement>): Serializable
