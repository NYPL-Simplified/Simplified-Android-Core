package org.nypl.simplified.app.reader.toc

import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarks
import java.io.Serializable

data class ReaderTOCParameters(
  val bookmarks: ReaderBookmarks,
  val tocElements: List<ReaderTOCElement>
) : Serializable
