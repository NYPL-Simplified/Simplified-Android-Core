package org.nypl.drm.core;

import java.io.File;

/**
 * The type of factories for producing Adobe Adept Connector instances.
 *
 * @see AdobeAdeptConnectorType
 */

public interface AdobeAdeptContentFilterFactoryType
{
  /**
   * <p>Retrieve an instance of the {@link AdobeAdeptContentFilterType}.</p>
   *
   * @param package_name    The application package name
   * @param package_version The application package version
   * @param res             A resource provider
   * @param net             A net provider
   * @param device_serial   The serial number of the device
   * @param device_name     The name of the device
   * @param app_storage     The path to application storage
   * @param xml_storage     The path to XML storage
   * @param book_path       The path to fulfilled books
   * @param temporary_dir   A directory usable for temporary private file
   *                        storage (such as the per-application Android
   *                        external cache directory).
   *
   * @return An instance of the connector
   *
   * @throws DRMException On any errors
   */

  AdobeAdeptContentFilterType get(
    String package_name,
    String package_version,
    AdobeAdeptResourceProviderType res,
    AdobeAdeptNetProviderType net,
    String device_serial,
    String device_name,
    File app_storage,
    File xml_storage,
    File book_path,
    File temporary_dir)
    throws DRMException;
}
