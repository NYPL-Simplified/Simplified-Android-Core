package org.nypl.simplified.app.player

import androidx.annotation.StyleRes
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.File
import java.io.Serializable
import java.net.URI

/**
 * Parameters for the audio book player.
 */

data class AudioBookPlayerParameters(

  /**
   * The current manifest file.
   */

  val manifestFile: File,

  /**
   * A URI that can be used to fetch a more up-to-date copy of the manifest.
   */

  val manifestURI: URI,

  /**
   * The book ID.
   */

  val bookID: BookID,

  /**
   * The OPDS entry for the book.
   */

  val opdsEntry: OPDSAcquisitionFeedEntry,

  /**
   * The theme to use for the activity.
   */

  @StyleRes
  val theme: Int
) : Serializable
