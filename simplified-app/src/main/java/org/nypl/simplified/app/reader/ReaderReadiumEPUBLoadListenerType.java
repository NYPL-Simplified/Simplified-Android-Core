package org.nypl.simplified.app.reader;

import org.readium.sdk.android.Container;

public interface ReaderReadiumEPUBLoadListenerType
{
  void onEPUBLoadFailed(
    Throwable x);

  void onEPUBLoadSucceeded(
    Container c);
}
