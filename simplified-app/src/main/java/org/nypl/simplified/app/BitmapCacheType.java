package org.nypl.simplified.app;

import java.net.URI;

import android.graphics.Bitmap;

import com.google.common.util.concurrent.ListenableFuture;

public interface BitmapCacheType
{
  ListenableFuture<Bitmap> get(
    final URI uri,
    final BitmapCacheListenerType p);
}
