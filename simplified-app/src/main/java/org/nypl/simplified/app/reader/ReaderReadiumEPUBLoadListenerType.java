package org.nypl.simplified.app.reader;

import org.readium.sdk.android.Container;

/**
 * The type of EPUB loading listeners.
 */

public interface ReaderReadiumEPUBLoadListenerType
{
  /**
   * The EPUB failed to load.
   */

  void onEPUBLoadFailed(
    Throwable x);

  /**
   * The EPUB loaded successfully.
   */

  void onEPUBLoadSucceeded(
    Container c);
}
