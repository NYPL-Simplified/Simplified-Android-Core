package org.nypl.simplified.books.covers

import android.graphics.Bitmap
import java.io.IOException
import java.net.URI

/**
 * The type of book cover generators.
 */

interface BookCoverGeneratorType {

  /**
   * Generate an image synchronously.
   *
   * @param uri The image URI
   * @param width The image width
   * @param height The image height
   *
   * @return A loaded bitmap
   *
   * @throws IOException On errors
   */

  @Throws(IOException::class)
  fun generateImage(
    uri: URI,
    width: Int,
    height: Int
  ): Bitmap

  /**
   * Generate a URI from the given title and author, suitable for use in cover
   * generation.
   *
   * @param title The title
   * @param author The author
   *
   * @return A URI
   */

  fun generateURIForTitleAuthor(
    title: String,
    author: String
  ): URI
}
