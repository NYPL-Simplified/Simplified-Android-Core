package org.nypl.simplified.viewer.epub.readium1;

/**
 * The type of asynchronous EPUB loaders.
 */

public interface ReaderReadiumEPUBLoaderType {

  /**
   * Attempt to load an EPUB.
   *
   * @param request The loading request
   * @param l       The EPUB result listener
   */

  void loadEPUB(
    ReaderReadiumEPUBLoadRequest request,
    ReaderReadiumEPUBLoadListenerType l);
}
