package org.nypl.simplified.app.catalog;

import android.graphics.Bitmap;
import android.net.Uri;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;
import java.net.URI;

/**
 * A Picasso request handler.
 */

public final class CatalogBookCoverGeneratorRequestHandler
  extends RequestHandler
{
  private final CatalogBookCoverGeneratorType generator;

  /**
   * Construct a request handler.
   *
   * @param in_generator The cover generator
   */

  public CatalogBookCoverGeneratorRequestHandler(
    final CatalogBookCoverGeneratorType in_generator)
  {
    this.generator = NullCheck.notNull(in_generator);
  }

  @Override public boolean canHandleRequest(
    final @Nullable Request data_mn)
  {
    final Request data = NullCheck.notNull(data_mn);
    final Uri u = data.uri;
    final URI uu = URI.create(u.toString());
    return "generated-cover".equals(uu.getScheme());
  }

  @Override public Result load(
    final @Nullable Request request_mn,
    final int network_policy)
    throws IOException
  {
    try {
      final Request request = NullCheck.notNull(request_mn);
      final Bitmap b = this.generator.generateImage(
        NullCheck.notNull(URI.create(request.uri.toString())),
        request.targetWidth,
        request.targetHeight);
      return new Result(b, LoadedFrom.MEMORY);
    } catch (final Throwable e) {
      throw new IOException(e);
    }
  }
}
