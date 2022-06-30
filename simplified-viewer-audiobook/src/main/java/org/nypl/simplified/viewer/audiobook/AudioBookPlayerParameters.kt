package org.nypl.simplified.viewer.audiobook

import org.nypl.simplified.accounts.api.AccountID
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
   * The user agent string used to make manifest requests.
   */

  val userAgent: String,

  /**
   * The current manifest content type.
   */

  val manifestContentType: String,

  /**
   * The current manifest file.
   */

  val manifestFile: File,

  /**
   * A URI that can be used to fetch a more up-to-date copy of the manifest.
   */

  val manifestURI: URI,

  /**
   * The account to which the book belongs.
   */

  val accountID: AccountID,

  /**
   * The book ID.
   */

  val bookID: BookID,

  /**
   * The OPDS entry for the book.
   */

  val opdsEntry: OPDSAcquisitionFeedEntry
) : Serializable

