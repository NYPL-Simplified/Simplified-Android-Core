package org.nypl.simplified.app;

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

/**
 * <p>
 * A file-cache based implementation of {@link BitmapCacheType}.
 * </p>
 * <p>
 * The cache is a fixed-size disk-based LRU cache that saves downloaded
 * images. Note that calling {@link #get(URI, BitmapCacheListenerType)}
 * multiple times for the same URI will (assuming the image downloaded
 * correctly) decode the image multiple times. In order to prevent repeated
 * decoding for frequently accessed images, an additional separate in-memory
 * cache should be used.
 * </p>
 */

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class BitmapFileCache implements
  BitmapCacheType
{
  private static final String TAG;

  static {
    TAG = "BFC";
  }

  /**
   * Attempt to decode a cached image. If the image is in the cache but cannot
   * be decoded, delete it.
   *
   * @param hash
   *          The hash of the URI
   * @return <code>null</code> if the image is not in the cache, or cannot be
   *         decoded.
   * @throws IOException
   *           On errors
   */

  private static @Nullable Bitmap decodeCachedImage(
    final DiskLruCache c,
    final String hash)
    throws IOException
  {
    final Snapshot s = c.get(hash);
    try {
      if (s == null) {
        return null;
      }

      final Bitmap r = BitmapFactory.decodeStream(s.getInputStream(0));
      if (r == null) {
        c.remove(hash);
        return null;
      }
      return r;
    } finally {
      if (s != null) {
        s.close();
      }
    }
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

  private static Bitmap getSynchronousActual(
    final URI uri,
    final PartialFunctionType<URI, InputStream, IOException> ref_t,
    final DiskLruCache c)
    throws IOException
  {
    final String hash = BitmapFileCache.hashURI(uri);

    Log.d(BitmapFileCache.TAG, String.format("fetching %s (%s)", uri, hash));

    /**
     * 1. If the image is already in the disk cache, attempt to decode it.
     *
     * 2. If it can be decoded, return it. Otherwise, if it cannot be decoded,
     * delete it.
     *
     * 3. Fetch and cache the file at the given URI.
     *
     * 4. If it can be decoded, return it. Otherwise, raise an error.
     */

    final Bitmap i0 = BitmapFileCache.decodeCachedImage(c, hash);
    if (i0 == null) {
      BitmapFileCache.fetchAndCacheFile(c, ref_t, uri, hash);
      final Bitmap i1 = BitmapFileCache.decodeCachedImage(c, hash);
      if (i1 == null) {
        throw new IOException("Could not decode cached file");
      }
      return i1;
    }

    return i0;
  }
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
  public static BitmapCacheType newCache(
    final ExecutorService in_e,
    final PartialFunctionType<URI, InputStream, IOException> in_transport,
    final File in_file,
    final int in_bytes)
    throws IOException
  {
    return new BitmapFileCache(in_e, in_transport, in_file, in_bytes);
  }

  private final DiskLruCache                                       cache;

  private final ListeningExecutorService                           exec;

  private final PartialFunctionType<URI, InputStream, IOException> transport;

  private BitmapFileCache(
    final ExecutorService e,
    final PartialFunctionType<URI, InputStream, IOException> in_transport,
    final File file,
    final int bytes)
    throws IOException
  {
    this.exec =
      NullCheck
        .notNull(MoreExecutors.listeningDecorator(NullCheck.notNull(e)));
    this.cache = NullCheck.notNull(DiskLruCache.open(file, 1, 1, bytes));
    this.transport = NullCheck.notNull(in_transport);
  }

  @Override public ListenableFuture<Bitmap> get(
    final URI uri,
    final BitmapCacheListenerType p)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(p);

    final PartialFunctionType<URI, InputStream, IOException> ref_t =
      this.transport;
    final DiskLruCache c = this.cache;

    final ListenableFuture<Bitmap> f =
      this.exec.submit(new Callable<Bitmap>() {
        @Override public Bitmap call()
          throws Exception
        {
          return BitmapFileCache.getSynchronousActual(uri, ref_t, c);
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
    final URI uri)
    throws IOException
  {
    return BitmapFileCache.getSynchronousActual(
      uri,
      this.transport,
      this.cache);
  }
}
