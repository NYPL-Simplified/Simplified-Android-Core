package org.nypl.simplified.ui.images

import android.content.Context
import com.squareup.picasso.Picasso.LoadedFrom.DISK
import com.squareup.picasso.Picasso.LoadedFrom.NETWORK
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import okio.Okio
import java.io.ByteArrayInputStream
import java.net.URL

/**
 * A Picasso request handler for account icons.
 *
 * Most account icons are using a `data:` URI scheme with a Base64 encoded PNG as the payload.
 */

class ImageAccountIconRequestHandler(
  private val context: Context
) : RequestHandler() {

  override fun canHandleRequest(data: Request): Boolean = true

  override fun load(
    request: Request,
    networkPolicy: Int
  ): Result {
    return when (request.uri.scheme) {
      "data" -> {
        val bitmap = ImageIconViews.imageFromBase64URI(request.uri.toString())
        if (bitmap != null) {
          Result(bitmap, DISK)
        } else {
          this.failQuietly()
        }
      }

      "simplified-asset" -> {
        val path = request.uri.schemeSpecificPart
        if (path != null) {
          Result(Okio.source(context.assets.open(path)), DISK)
        } else {
          this.failQuietly()
        }
      }

      else -> {
        Result(Okio.source(URL(request.uri.toString()).openStream()), NETWORK)
      }
    }
  }

  private fun failQuietly() =
    Result(Okio.source(ByteArrayInputStream(ByteArray(0))), DISK)
}
