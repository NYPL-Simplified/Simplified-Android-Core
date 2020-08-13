package org.nypl.simplified.books.covers

import android.graphics.BitmapFactory
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.bundled.api.BundledURIs
import java.io.IOException
import java.net.URI

class BookCoverBundledRequestHandler(
  private val bundledContentResolver: BundledContentResolverType
) : RequestHandler() {

  override fun canHandleRequest(
    data: Request
  ): Boolean {
    return BundledURIs.isBundledURI(URI.create(data.uri.toString()))
  }

  override fun load(
    request: Request,
    networkPolicy: Int
  ): Result =
    try {
      val uri = URI.create(request.uri.toString())
      this.bundledContentResolver.resolve(uri).use { stream ->
        val bitmap = BitmapFactory.decodeStream(stream)
        return Result(bitmap, Picasso.LoadedFrom.MEMORY)
      }
    } catch (e: Throwable) {
      throw IOException(e)
    }
}
