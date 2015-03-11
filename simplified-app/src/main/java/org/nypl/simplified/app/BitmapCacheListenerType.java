package org.nypl.simplified.app;

import android.graphics.Bitmap;

public interface BitmapCacheListenerType<K>
{
  void onSuccess(
    final K key,
    final Bitmap b);

  void onFailure(
    final K key,
    final Throwable x);
}
