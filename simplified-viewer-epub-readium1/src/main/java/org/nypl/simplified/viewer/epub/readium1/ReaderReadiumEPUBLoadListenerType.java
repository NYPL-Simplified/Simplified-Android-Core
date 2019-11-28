package org.nypl.simplified.viewer.epub.readium1;

import org.readium.sdk.android.Container;

/**
 * The type of EPUB loading listeners.
 */

public interface ReaderReadiumEPUBLoadListenerType
{
  /**
   * The EPUB failed to load.
   *
   * @param x The raised exception
   */

  void onEPUBLoadFailed(
    Throwable x);

  /**
   * The EPUB loaded successfully.
   *
   * @param c The EPUB container
   */

  void onEPUBLoadSucceeded(
    Container c);
}
