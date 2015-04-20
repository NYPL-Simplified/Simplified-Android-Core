package org.nypl.simplified.app.reader;

import org.readium.sdk.android.Container;

public interface ReaderBookLoadedListenerType
{
  void onBookLoadFailed(
    Throwable x);

  void onBookLoadSucceeded(
    Container c);
}
