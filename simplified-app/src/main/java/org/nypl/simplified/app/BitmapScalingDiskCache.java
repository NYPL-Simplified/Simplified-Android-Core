package org.nypl.simplified.app;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Editor;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;

public final class BitmapScalingDiskCache implements BitmapCacheScalingType
{
  private static final class CachedImage
  {
    int                   height;
    @Nullable InputStream stream;
    int                   width;
  }

  private static Bitmap decode(
    final URI uri,
    final BitmapScalingOptions opts,
    final String hash,
    final CachedImage ci)
    throws IOException
  {
    final Options o = new Options();

    o.inPreferredConfig = Bitmap.Config.RGB_565;
    switch (opts.getType()) {
      case TYPE_SCALE_PRESERVE:
      {
        break;
      }
      case TYPE_SCALE_SIZE_HINT:
      {
        o.inSampleSize =
          BitmapScalingDiskCache.getSampleSize(
            ci,
            opts.getWidth(),
            opts.getHeight());
        break;
      }
    }

    final InputStream is = NullCheck.notNull(ci.stream);
    try {
      final Bitmap b = BitmapFactory.decodeStream(is, null, o);
      if (b == null) {
        throw BitmapScalingDiskCache.decodeFailed(uri, hash);
      }
      return b;
    } finally {
      is.close();
    }
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
    final int want_width,
    final int want_height)
  {
    return 4;
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

  public static BitmapCacheScalingType newCache(
    final ExecutorService e,
    final PartialFunctionType<URI, InputStream, IOException> in_transport,
    final File in_file,
    final long size)
    throws IOException
  {
    final DiskLruCache in_cache = DiskLruCache.open(in_file, 1, 1, size);
    return new BitmapScalingDiskCache(e, in_cache, in_transport);
  }

  private final DiskLruCache                                       cache;
  private final ListeningExecutorService                           exec;
  private final PartialFunctionType<URI, InputStream, IOException> transport;
  private static final String                                      TAG;

  static {
    TAG = "BSDC";
  }

  private BitmapScalingDiskCache(
    final ExecutorService e,
    final DiskLruCache in_cache,
    final PartialFunctionType<URI, InputStream, IOException> in_transport)
  {
    this.exec =
      NullCheck
        .notNull(MoreExecutors.listeningDecorator(NullCheck.notNull(e)));
    this.cache = NullCheck.notNull(in_cache);
    this.transport = NullCheck.notNull(in_transport);
  }

  @Override public ListenableFuture<Bitmap> get(
    final URI uri,
    final BitmapScalingOptions opts,
    final BitmapCacheListenerType p)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(opts);
    NullCheck.notNull(p);

    final ListenableFuture<Bitmap> f =
      this.exec.submit(new Callable<Bitmap>() {
        @Override public Bitmap call()
          throws Exception
        {
          return BitmapScalingDiskCache.this.getSynchronous(uri, opts);
        }
      });

    Futures.addCallback(f, new FutureCallback<Bitmap>() {
      @Override public void onFailure(
        final @Nullable Throwable t)
      {
        p.onFailure(NullCheck.notNull(t));
      }

      @Override public void onSuccess(
        final @Nullable Bitmap result)
      {
        p.onSuccess(NullCheck.notNull(result));
      }
    }, this.exec);

    return NullCheck.notNull(f);
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
    final BitmapScalingOptions opts)
    throws IOException
  {
    final String hash = BitmapScalingDiskCache.hashURI(uri);
    Log
      .d(BitmapScalingDiskCache.TAG, String.format("get %s (%s)", uri, hash));

    final CachedImage ci0 = this.isCached(hash);
    if (ci0 == null) {
      BitmapScalingDiskCache.fetchAndCacheFile(
        this.cache,
        this.transport,
        uri,
        hash);

      final CachedImage ci1 = this.isCached(hash);
      if (ci1 == null) {
        throw BitmapScalingDiskCache.decodeFailed(uri, hash);
      }

      return BitmapScalingDiskCache.decode(uri, opts, hash, ci1);
    }

    return BitmapScalingDiskCache.decode(uri, opts, hash, ci0);
  }

  private @Nullable CachedImage isCached(
    final String hash)
    throws IOException
  {
    final Snapshot s = this.cache.get(hash);
    if (s == null) {
      return null;
    }

    /**
     * It's necessary to parse the headers of the image to determine the image
     * bounds, and then seek the stream back to the beginning to parse the
     * actual image.
     *
     * For PNG images, the IHDR chunk holds the image bounds and is required
     * to appear at the start of the file, so will be within the 8192 byte
     * region. Unfortunately, this is not guaranteed to be true of JPEG files.
     */

    final BufferedInputStream is =
      new BufferedInputStream(s.getInputStream(0), 8192);
    is.mark(8192);

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
