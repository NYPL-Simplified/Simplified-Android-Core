package org.nypl.simplified.app.images

import com.squareup.picasso.Picasso

/**
 * An image loader used for image resources that are not book covers.
 *
 * @see [org.nypl.simplified.books.covers.BookCoverProviderType]
 * @see [org.nypl.simplified.books.covers.BookCoverGeneratorType]
 */

interface ImageLoaderType {

  /**
   * The Picasso image loader.
   */

  val loader: Picasso
}
