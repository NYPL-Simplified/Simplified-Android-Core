package org.nypl.simplified.app.reader;

import org.nypl.simplified.opds.core.DRMLicensor;

import java.io.File;

/**
 * The type of asynchronous EPUB loaders.
 */

public interface ReaderReadiumEPUBLoaderType
{
  /**
   * Attempt to load an EPUB.
   * @param f The EPUB file
   * @param l The EPUB result listener
   * @param drm_type DRM Type (Adobe, URMS, LCP ...)
   */

  void loadEPUB(
    File f,
    ReaderReadiumEPUBLoadListenerType l, DRMLicensor.DRM drm_type);
}
