package org.nypl.simplified.app;

import java.io.IOException;
import java.net.URI;
import java.util.WeakHashMap;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

@SuppressWarnings("synthetic-access") public final class BitmapCacheScalingMemoryProxy implements
  BitmapCacheScalingType
{
  private static final class Cache extends LruCache<Key, Bitmap>
  {
    private final BitmapCacheScalingType      base;
    private final WeakHashMap<URI, Throwable> errors;

    public Cache(
      final int in_size,
      final BitmapCacheScalingType in_base)
    {
      super(in_size);
      this.errors = new WeakHashMap<URI, Throwable>();
      this.base = NullCheck.notNull(in_base);
    }

    @Override protected @Nullable Bitmap create(
      final @Nullable Key key)
    {
      try {
        assert key != null;
        return this.base.getSynchronous(key.uri, key.opts);
      } catch (final IOException e) {
        this.errors.put(key.uri, e);
        return null;
      }
    }

    @Override protected int sizeOf(
      final @Nullable Key key,
      final @Nullable Bitmap value)
    {
      NullCheck.notNull(key);
      return NullCheck.notNull(value).getAllocationByteCount();
    }
  }

  private static final class Key
  {
    private final BitmapDisplaySizeType opts;
    private final URI                   uri;

    private Key(
      final URI in_uri,
      final BitmapDisplaySizeType in_opts)
    {
      this.uri = NullCheck.notNull(in_uri);
      this.opts = NullCheck.notNull(in_opts);
    }

    @Override public boolean equals(
      final @Nullable Object obj)
    {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (this.getClass() != obj.getClass()) {
        return false;
      }
      final Key other = (Key) obj;
      return this.uri.equals(other.uri);
    }

    @Override public int hashCode()
    {
      return this.uri.hashCode();
    }
  }

  public static BitmapCacheScalingType newCache(
    final BitmapCacheScalingType in_base,
    final int size)
  {
    return new BitmapCacheScalingMemoryProxy(in_base, size);
  }

  private final Cache cache;

  private BitmapCacheScalingMemoryProxy(
    final BitmapCacheScalingType in_base,
    final int in_size)
  {
    this.cache = new Cache(in_size, NullCheck.notNull(in_base));
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
    final URI uri,
    final BitmapDisplaySizeType opts)
    throws IOException
  {
    final Bitmap r = this.cache.get(new Key(uri, opts));
    if (r == null) {
      throw new IOException(this.cache.errors.get(uri));
    }
    return r;
  }
}
