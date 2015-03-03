package org.nypl.simplified.app;

import java.io.IOException;
import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.graphics.Bitmap;
import android.util.Log;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

public final class CatalogAcquisitionImageCache implements
  CatalogAcquisitionImageCacheType
{
  private static final String TAG;

  static {
    TAG = "CAIC";
  }

  private static int getHeight(
    final BitmapDisplaySizeType size)
  {
    return size.matchSize(
      new BitmapDisplaySizeMatcherType<Integer, UnreachableCodeException>() {
        @Override public Integer matchHeightAspectPreserving(
          final BitmapDisplayHeightPreserveAspect s)
        {
          return Integer.valueOf(s.getHeight());
        }
      }).intValue();
  }

  public static CatalogAcquisitionImageCacheType newCache(
    final CatalogAcquisitionImageGeneratorType in_gen,
    final BitmapCacheScalingType in_cache)
  {
    return new CatalogAcquisitionImageCache(in_gen, in_cache);
  }

  private final BitmapCacheScalingType               cache;
  private final CatalogAcquisitionImageGeneratorType gen;

  private CatalogAcquisitionImageCache(
    final CatalogAcquisitionImageGeneratorType in_gen,
    final BitmapCacheScalingType in_cache)
  {
    this.gen = NullCheck.notNull(in_gen);
    this.cache = NullCheck.notNull(in_cache);
  }

  private Bitmap generate(
    final OPDSAcquisitionFeedEntry e,
    final BitmapDisplaySizeType size)
  {
    return this.gen.generateImage(
      e,
      CatalogAcquisitionImageCache.getHeight(size));
  }

  private Bitmap getActual(
    final OPDSAcquisitionFeedEntry e,
    final BitmapDisplaySizeType size)
  {
    final OptionType<URI> thumb_opt = e.getThumbnail();
    if (thumb_opt.isNone()) {
      return this.generate(e, size);
    }

    final Some<URI> some = (Some<URI>) thumb_opt;
    final URI uri = some.get();
    try {
      return this.load(uri, size);
    } catch (final Exception x) {
      Log.e(
        CatalogAcquisitionImageCache.TAG,
        String.format("failed to load image (%s), generating instead", uri),
        x);
      return this.generate(e, size);
    }
  }

  @Override public Bitmap getSynchronous(
    final OPDSAcquisitionFeedEntry e,
    final BitmapDisplaySizeType size)
  {
    NullCheck.notNull(e);
    NullCheck.notNull(size);
    return this.getActual(e, size);
  }

  private Bitmap load(
    final URI uri,
    final BitmapDisplaySizeType size)
    throws IOException
  {
    Log.d(CatalogAcquisitionImageCache.TAG, String.format("loading %s", uri));
    final Bitmap r = this.cache.getSynchronous(uri, size);
    Log.d(
      CatalogAcquisitionImageCache.TAG,
      String.format(
        "returned image (%s) is (%d x %d)",
        uri,
        r.getWidth(),
        r.getHeight()));
    return r;
  }
}
