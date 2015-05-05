package org.nypl.simplified.app.reader;

import java.io.File;

/**
 * The type of asynchronous EPUB loaders.
 */

public interface ReaderReadiumEPUBLoaderType
{
  void loadEPUB(
    File f,
    ReaderReadiumEPUBLoadListenerType l);
}
