package org.nypl.simplified.ui.images

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import com.google.common.base.Preconditions

import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.URI

/**
 * A class to work around the fact that most of the Android API can't open its own assets
 * via URIs. This allows URIs of the form `simplified-asset:file.png` to load `file.png`
 * from the application's assets.
 *
 * @see [https://stackoverflow.com/a/7533725](https://stackoverflow.com/a/7533725)
 */

object ImageIconViews {

  private val LOG = LoggerFactory.getLogger(ImageIconViews::class.java)

  /**
   * Load the image at the given URI into the given icon view.
   *
   * @param assets The current asset manager
   * @param iconView The icon view
   * @param image The image URI
   */

  fun configureIconViewFromURI(
    assets: AssetManager,
    iconView: ImageView,
    image: URI
  ) {
    return if ("simplified-asset" == image.scheme) {
      configureFromAsset(image, assets, iconView)
    } else {
      iconView.setImageURI(Uri.parse(image.toString()))
    }
  }

  /**
   * Decode an image from a Base64 data URI.
   *
   * @param text The data URI
   */

  fun imageFromBase64URI(text: String): Bitmap? {
    Preconditions.checkArgument(
      text.startsWith("data:"),
      "Base64 URI must begin with 'data:'"
    )

    val comma = text.indexOf(',')
    if (comma != -1) {
      val base64String = text.substring(comma)
      val data = Base64.decode(base64String, 0)
      return BitmapFactory.decodeByteArray(data, 0, data.size)
    }
    return null
  }

  private fun configureFromAsset(
    image: URI,
    assets: AssetManager,
    iconView: ImageView
  ) {
    val path = image.schemeSpecificPart
    LOG.debug("opening image asset: {}", path)
    try {
      assets.open(path).use { stream ->
        iconView.setImageDrawable(Drawable.createFromStream(stream, path))
      }
    } catch (e: IOException) {
      LOG.error("could not open image asset: {}: ", image, e)
    }
  }
}
