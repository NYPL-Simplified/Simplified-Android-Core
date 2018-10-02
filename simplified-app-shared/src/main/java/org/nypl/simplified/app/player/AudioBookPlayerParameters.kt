package org.nypl.simplified.app.player

import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.File
import java.io.Serializable
import java.net.URI

data class AudioBookPlayerParameters(
  val manifestFile: File,
  val manifestURI: URI,
  val bookID: BookID,
  val opdsEntry: OPDSAcquisitionFeedEntry) : Serializable
