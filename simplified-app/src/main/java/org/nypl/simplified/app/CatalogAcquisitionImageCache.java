package org.nypl.simplified.app;

import java.io.IOException;
import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.util.Log;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

public final class CatalogAcquisitionImageCache implements
  CatalogAcquisitionImageCacheType
{
  private static final String TAG;

  static {
    TAG = "CAIC";
  }

  public static CatalogAcquisitionImageCacheType newCache(
    final BitmapCacheScalingType in_cache)
  {
    return new CatalogAcquisitionImageCache(in_cache);
  }

  private final BitmapCacheScalingType cache;

  private CatalogAcquisitionImageCache(
    final BitmapCacheScalingType in_cache)
  {
    this.cache = NullCheck.notNull(in_cache);
  }

  private Bitmap generate(
    final OPDSAcquisitionFeedEntry e)
  {
    Log.d(
      CatalogAcquisitionImageCache.TAG,
      String.format("generating %s", e.getID()));

    final Bitmap b = Bitmap.createBitmap(64, 64, Config.RGB_565);
    return b;
  }

  @Override public Bitmap getSynchronous(
    final OPDSAcquisitionFeedEntry e,
    final BitmapDisplaySizeType size)
  {
    NullCheck.notNull(e);
    NullCheck.notNull(size);
    return this.getActual(e, size);
  }

  private Bitmap getActual(
    final OPDSAcquisitionFeedEntry e,
    final BitmapDisplaySizeType size)
  {
    final OptionType<URI> thumb_opt = e.getThumbnail();
    if (thumb_opt.isNone()) {
      return this.generate(e);
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
      return this.generate(e);
    }
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
