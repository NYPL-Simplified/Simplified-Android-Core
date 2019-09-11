package org.nypl.simplified.app.images

import com.squareup.picasso.Picasso.LoadedFrom.DISK
import com.squareup.picasso.Picasso.LoadedFrom.NETWORK
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import java.io.ByteArrayInputStream
import java.net.URL

/**
 * A Picasso request handler for account icons.
 *
 * Most account icons are using a `data:` URI scheme with a Base64 encoded PNG as the payload.
 */

class ImageAccountIconRequestHandler : RequestHandler() {

  override fun canHandleRequest(data: Request): Boolean = true

  override fun load(request: Request, networkPolicy: Int): Result {
    return if (request.uri.scheme == "data") {
      val bitmap = ImageIconViews.imageFromBase64URI(request.uri.toString())
      if (bitmap != null) {
        Result(bitmap, DISK)
      } else {
        this.failQuietly()
      }
    } else {
      return Result(URL(request.uri.toString()).openStream(), NETWORK)
    }
  }

  private fun failQuietly() =
    Result(ByteArrayInputStream(ByteArray(0)), DISK)
}
