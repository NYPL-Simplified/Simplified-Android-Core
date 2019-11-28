package org.nypl.simplified.viewer.epub.readium1.toc

import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarks
import java.io.Serializable

data class ReaderTOCParameters(
  val bookmarks: ReaderBookmarks,
  val tocElements: List<ReaderTOCElement>
) : Serializable
