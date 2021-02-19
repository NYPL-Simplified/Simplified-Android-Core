package org.nypl.drm.core;

/**
 * A client that can, when requested, load rights data for a book.
 */

public interface AdobeAdeptContentRightsClientType
{
  /**
   * @param path The path to the currently loaded book
   *
   * @return Rights data for the current book.
   */

  byte[] getRightsData(String path);
}
