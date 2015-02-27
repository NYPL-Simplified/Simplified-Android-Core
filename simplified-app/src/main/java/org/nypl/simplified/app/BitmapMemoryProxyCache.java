package org.nypl.simplified.app;

import java.io.IOException;
import java.net.URI;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;

/**
 * A fixed-size in-memory cached intended to cache the results of a slower
 * (disk-based, for example) cache.
 */

public final class BitmapMemoryProxyCache implements BitmapCacheType
{
  private static final class Cache extends LruCache<URI, Bitmap>
  {
    private final BitmapCacheType             base;
    private final WeakHashMap<URI, Throwable> errors;

    public Cache(
      final int maxSize,
      final BitmapCacheType in_base)
    {
      super(maxSize);
      this.base = NullCheck.notNull(in_base);
      this.errors = new WeakHashMap<URI, Throwable>();
    }

    @Override protected @Nullable Bitmap create(
      final @Nullable URI uri)
    {
      Log.d(BitmapMemoryProxyCache.TAG, String.format("creating %s", uri));
      try {
        return this.base.getSynchronous(NullCheck.notNull(uri));
      } catch (final IOException e) {
        this.errors.put(uri, e);
        return null;
      }
    }

    @Override protected int sizeOf(
      final @Nullable URI key,
      final @Nullable Bitmap value)
    {
      assert key != null;
      assert value != null;
      return value.getAllocationByteCount();
    }

    public Throwable takeRecordedError(
      final URI uri)
    {
      return NullCheck.notNull(this.errors.get(uri));
    }
  }

  private static final String TAG;

  static {
    TAG = "BMPC";
  }

  public static BitmapCacheType newCache(
    final ExecutorService e,
    final BitmapCacheType in_base,
    final int in_size)
  {
    return new BitmapMemoryProxyCache(e, in_base, in_size);
  }

  private final Cache                    cache;
  private final ListeningExecutorService exec;

  private BitmapMemoryProxyCache(
    final ExecutorService e,
    final BitmapCacheType in_base,
    final int in_size)
  {
    this.exec =
      NullCheck
        .notNull(MoreExecutors.listeningDecorator(NullCheck.notNull(e)));
    this.cache = new Cache(in_size, NullCheck.notNull(in_base));
  }

  @Override public ListenableFuture<Bitmap> get(
    final URI uri,
    final BitmapCacheListenerType p)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(p);

    Log.d(BitmapMemoryProxyCache.TAG, String.format("fetching %s", uri));

    final Cache c = this.cache;
    final ListenableFuture<Bitmap> f =
      this.exec.submit(new Callable<Bitmap>() {
        @Override public Bitmap call()
          throws Exception
        {
          final Bitmap r = c.get(uri);
          if (r == null) {
            throw new Exception(c.takeRecordedError(uri));
          }
          return r;
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
    return this.cache.maxSize();
  }

  @Override public Bitmap getSynchronous(
    final URI uri)
    throws IOException
  {
    throw new UnimplementedCodeException();
  }
}
