package org.nypl.simplified.app.reader;

import java.io.File;

/**
 * The type of asynchronous EPUB loaders.
 */

public interface ReaderReadiumEPUBLoaderType
{
  /**
   * Attempt to load an EPUB.
   *
   * @param f The EPUB file
   * @param l The EPUB result listener
   */

  void loadEPUB(
    File f,
    ReaderReadiumEPUBLoadListenerType l);
}
