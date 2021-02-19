package org.nypl.drm.core;

/**
 * The type of factories for producing Adobe Adept Connector instances.
 *
 * @see AdobeAdeptConnectorType
 */

public interface AdobeAdeptConnectorFactoryType
{
  /**
   * <p>Retrieve an instance of the {@link AdobeAdeptConnectorType}.</p>
   *
   * @param p Parameters for the connector
   *
   * @return An instance of the connector
   *
   * @throws DRMException On any errors
   */

  AdobeAdeptConnectorType get(
    AdobeAdeptConnectorParameters p)
    throws DRMException;
}
