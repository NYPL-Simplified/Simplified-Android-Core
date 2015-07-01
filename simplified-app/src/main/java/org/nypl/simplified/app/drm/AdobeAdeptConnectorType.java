package org.nypl.simplified.app.drm;

/**
 * The interface exposed by the Adobe Adept Connector (the DRM component of
 * the Adobe RMSDK).
 */

interface AdobeAdeptConnectorType
{
  void authorizeDevice(
    final String vendor,
    final String user,
    final String password);
}
