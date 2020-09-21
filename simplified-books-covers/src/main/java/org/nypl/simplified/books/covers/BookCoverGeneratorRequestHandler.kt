package org.nypl.simplified.books.covers

import com.io7m.jnull.NullCheck
import com.io7m.jnull.Nullable
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import java.io.IOException
import java.net.URI

/**
 * A Picasso request handler.
 *
 * This delegates requests for URIs that have a scheme "generated-cover" to the given
 * book generator instead of trying to load data from the network or disk.
 */

class BookCoverGeneratorRequestHandler(
  private val generator: BookCoverGeneratorType
) : RequestHandler() {

  override fun canHandleRequest(@Nullable requestNullable: Request): Boolean {
    val data = NullCheck.notNull(requestNullable)
    val uri = URI.create(data.uri.toString())
    return "generated-cover" == uri.scheme
  }

  @Throws(IOException::class)
  override fun load(
    @Nullable requestNullable: Request,
    networkPolicy: Int
  ): Result {
    try {
      val request = NullCheck.notNull(requestNullable)
      val bitmap = this.generator.generateImage(
        NullCheck.notNull(URI.create(request.uri.toString())),
        request.targetWidth,
        request.targetHeight
      )
      return Result(bitmap, LoadedFrom.MEMORY)
    } catch (e: Throwable) {
      throw IOException(e)
    }
  }
}
