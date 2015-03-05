package org.nypl.simplified.app;

import android.graphics.Bitmap;

public interface BitmapCacheListenerType
{
  void onSuccess(
    final Bitmap b);

  void onFailure(
    final Throwable x);
}
