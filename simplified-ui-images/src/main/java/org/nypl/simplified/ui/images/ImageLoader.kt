package org.nypl.simplified.ui.images

import android.content.Context
import com.squareup.picasso.Picasso

/**
 * The default image loader implementation.
 */

class ImageLoader private constructor(
  override val loader: Picasso
) : ImageLoaderType {

  companion object {

    /**
     * Create a new image loader.
     */

    fun create(
      context: Context
    ): ImageLoaderType {
      val localImageLoader =
        Picasso.Builder(context)
          .indicatorsEnabled(false)
          .loggingEnabled(false)
          .addRequestHandler(ImageAccountIconRequestHandler(context))
          .build()

      return ImageLoader(localImageLoader)
    }
  }
}
