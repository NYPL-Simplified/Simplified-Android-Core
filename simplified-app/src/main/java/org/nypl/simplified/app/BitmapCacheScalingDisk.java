package org.nypl.simplified.app;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.jtensors.VectorM2I;
import com.io7m.junreachable.UnreachableCodeException;
import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Editor;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;

public final class BitmapCacheScalingDisk implements
  BitmapCacheScalingDiskType
{
  private static final class CachedImage
  {
    int                   height;
    @Nullable InputStream stream;
    int                   width;
  }

  private static Bitmap decode(
    final MemoryControllerType mem,
    final URI uri,
    final BitmapDisplaySizeType opts,
    final String hash,
    final CachedImage ci)
    throws IOException
  {
    final Options o = new Options();

    o.inPreferredConfig = Bitmap.Config.RGB_565;
    o.inSampleSize = BitmapCacheScalingDisk.getSampleSize(ci, opts);

    final InputStream is = NullCheck.notNull(ci.stream);
    try {
      final Bitmap b_large = BitmapFactory.decodeStream(is, null, o);
      if (b_large == null) {
        throw BitmapCacheScalingDisk.decodeFailed(uri, hash);
      }

      final VectorM2I new_size = new VectorM2I();
      BitmapCacheScalingDisk.getScaledSize(
        mem,
        new_size,
        b_large.getWidth(),
        b_large.getHeight(),
        opts);

      final Bitmap b_small =
        NullCheck.notNull(Bitmap.createScaledBitmap(
          b_large,
          new_size.getXI(),
          new_size.getYI(),
          true));

      return b_small;
    } finally {
      is.close();
    }
  }

  private static void getScaledSize(
    final MemoryControllerType mem,
    final VectorM2I new_size,
    final int current_width,
    final int current_height,
    final BitmapDisplaySizeType size)
  {
    size
      .matchSize(new BitmapDisplaySizeMatcherType<Unit, UnreachableCodeException>() {
        @Override public Unit matchHeightAspectPreserving(
          final BitmapDisplayHeightPreserveAspect dh)
        {
          final double req_height = dh.getHeight();
          final double cur_height = current_height;
          final double ratio = req_height / cur_height;
          final double res_width = current_width * ratio;

          final int iw;
          final int ih;
          if (mem.memoryIsSmall()) {
            iw = (int) res_width;
            ih = (int) req_height;
          } else {
            iw = (int) res_width;
            ih = (int) req_height;
          }

          new_size.set2I(iw, ih);
          return Unit.unit();
        }
      });
  }

  private static IOException decodeFailed(
    final URI uri,
    final String hash)
  {
    return new IOException(String.format(
      "Unable to decode image (%s / %s)",
      uri,
      hash));
  }

  /**
   * Fetch and cache the given file, raising errors on I/O errors.
   *
   * @param hash
   *          The hash of the URI.
   * @throws IOException
   *           On errors
   */

  private static void fetchAndCacheFile(
    final DiskLruCache c,
    final PartialFunctionType<URI, InputStream, IOException> transport,
    final URI uri,
    final String hash)
    throws IOException
  {
    Log.d(
      BitmapCacheScalingDisk.TAG,
      String.format("fetch %s (%s)", uri, hash));

    final Editor e = c.edit(hash);
    final OutputStream os = e.newOutputStream(0);
    try {
      final InputStream is = transport.call(uri);
      try {
        final byte[] b = new byte[8192];
        while (true) {
          final int r = is.read(b, 0, 8192);
          if (r == -1) {
            break;
          }
          os.write(b, 0, r);
        }
        e.commit();
      } finally {
        is.close();
      }
    } finally {
      os.close();
    }
  }

  /**
   * Calculate a sampling size to reduce the size of the bitmap upon loading.
   * The given width and height are used as a hint; the resulting image will
   * likely be somewhat larger than the given size.
   */

  private static int getSampleSize(
    final CachedImage ci,
    final BitmapDisplaySizeType opts)
  {
    return opts.matchSize(
      new BitmapDisplaySizeMatcherType<Integer, UnreachableCodeException>() {
        @Override public Integer matchHeightAspectPreserving(
          final BitmapDisplayHeightPreserveAspect dh)
        {
          /**
           * Note that the image may be smaller than the requested display
           * height.
           */

          final int req_height = dh.getHeight();
          final int height = ci.height;
          int ss = 1;

          if (height > req_height) {
            for (;;) {
              if ((height / (ss * 2)) <= req_height) {
                break;
              }
              ss *= 2;
            }
          }

          return Integer.valueOf(ss);
        }
      }).intValue();
  }

  /**
   * @return The SHA1 hash of the given URI.
   */

  private static String hashURI(
    final URI uri)
  {
    try {
      final MessageDigest d = MessageDigest.getInstance("SHA1");
      final byte[] r = d.digest(uri.toString().getBytes());
      final StringBuilder sb = new StringBuilder();
      for (final byte b : r) {
        sb.append(String.format("%02x", b));
      }
      return NullCheck.notNull(sb.toString());
    } catch (final NoSuchAlgorithmException e) {
      throw new UnreachableCodeException(e);
    }
  }

  public static BitmapCacheScalingDiskType newCache(
    final ListeningExecutorService e,
    final PartialFunctionType<URI, InputStream, IOException> in_transport,
    final MemoryControllerType in_mem,
    final File in_file,
    final long size)
    throws IOException
  {
    final DiskLruCache in_cache = DiskLruCache.open(in_file, 1, 1, size);
    return new BitmapCacheScalingDisk(e, in_cache, in_transport, in_mem);
  }

  private final MemoryControllerType                               memory;
  private final DiskLruCache                                       cache;
  private final ListeningExecutorService                           exec;
  private final PartialFunctionType<URI, InputStream, IOException> transport;
  private static final String                                      TAG;

  static {
    TAG = "BSDC";
  }

  private BitmapCacheScalingDisk(
    final ListeningExecutorService e,
    final DiskLruCache in_cache,
    final PartialFunctionType<URI, InputStream, IOException> in_transport,
    final MemoryControllerType in_mem)
  {
    this.exec = NullCheck.notNull(e);
    this.cache = NullCheck.notNull(in_cache);
    this.transport = NullCheck.notNull(in_transport);
    this.memory = NullCheck.notNull(in_mem);
  }

  @Override public long getSizeCurrent()
  {
    return this.cache.size();
  }

  @Override public long getSizeMaximum()
  {
    return this.cache.getMaxSize();
  }

  @Override public Bitmap getSynchronous(
    final URI uri,
    final BitmapDisplaySizeType size)
    throws IOException
  {
    final String hash = BitmapCacheScalingDisk.hashURI(uri);
    Log
      .d(BitmapCacheScalingDisk.TAG, String.format("get %s (%s)", uri, hash));

    final CachedImage ci0 = this.isCached(hash);
    if (ci0 == null) {
      BitmapCacheScalingDisk.fetchAndCacheFile(
        this.cache,
        this.transport,
        uri,
        hash);

      final CachedImage ci1 = this.isCached(hash);
      if (ci1 == null) {
        throw BitmapCacheScalingDisk.decodeFailed(uri, hash);
      }

      return BitmapCacheScalingDisk.decode(this.memory, uri, size, hash, ci1);
    }

    return BitmapCacheScalingDisk.decode(this.memory, uri, size, hash, ci0);
  }

  private @Nullable CachedImage isCached(
    final String hash)
    throws IOException
  {
    final Snapshot s = this.cache.get(hash);
    if (s == null) {
      return null;
    }

    final int buffer_size = (int) (Math.pow(10, 3) * 64);

    /**
     * It's necessary to parse the headers of the image to determine the image
     * bounds, and then seek the stream back to the beginning to parse the
     * actual image.
     *
     * For PNG images, the IHDR chunk holds the image bounds and is required
     * to appear at the start of the file, so will be within the
     * <tt>buffer_size</tt> byte region. Unfortunately, this is not guaranteed
     * to be true of JPEG files.
     */

    final BufferedInputStream is =
      new BufferedInputStream(s.getInputStream(0), buffer_size);
    is.mark(buffer_size);

    final Options os = new Options();
    os.inJustDecodeBounds = true;
    BitmapFactory.decodeStream(is, null, os);
    if (os.outHeight == -1) {
      return null;
    }

    is.reset();
    final CachedImage ci = new CachedImage();
    ci.height = os.outHeight;
    ci.width = os.outWidth;
    ci.stream = is;
    return ci;
  }
}
