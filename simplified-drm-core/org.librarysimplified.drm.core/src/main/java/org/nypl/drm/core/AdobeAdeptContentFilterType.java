package org.nypl.drm.core;

/**
 * The interface exposed by the Readium Adobe Adept content filter.
 */

public interface AdobeAdeptContentFilterType
{
  /**
   * Register the current content filter.
   *
   * @param c A rights client that can produce book rights on demand
   */

  void registerFilter(AdobeAdeptContentRightsClientType c);
}
