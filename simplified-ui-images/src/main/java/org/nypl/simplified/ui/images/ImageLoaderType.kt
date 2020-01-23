package org.nypl.simplified.ui.images

import com.squareup.picasso.Picasso

/**
 * An image loader used for image resources that are not book covers.
 */

interface ImageLoaderType {

  /**
   * The Picasso image loader.
   */

  val loader: Picasso
}
