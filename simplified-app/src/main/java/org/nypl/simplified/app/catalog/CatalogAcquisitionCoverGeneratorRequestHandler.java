package org.nypl.simplified.app.catalog;

import java.io.IOException;
import java.net.URI;

import android.graphics.Bitmap;
import android.net.Uri;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

public final class CatalogAcquisitionCoverGeneratorRequestHandler extends
  RequestHandler
{
  private final CatalogAcquisitionCoverGeneratorType generator;

  public CatalogAcquisitionCoverGeneratorRequestHandler(
    final CatalogAcquisitionCoverGeneratorType in_generator)
  {
    this.generator = NullCheck.notNull(in_generator);
  }

  @Override public boolean canHandleRequest(
    final @Nullable Request data)
  {
    assert data != null;
    final Uri u = data.uri;
    final URI uu = URI.create(u.toString());
    if ("generated-cover".equals(uu.getScheme())) {
      return true;
    }
    return false;
  }

  @Override public Result load(
    final @Nullable Request request,
    final int networkPolicy)
    throws IOException
  {
    assert request != null;
    final Bitmap b =
      this.generator.generateImage(
        URI.create(request.uri.toString()),
        request.targetWidth,
        request.targetHeight);
    return new Result(b, LoadedFrom.MEMORY);
  }
}
