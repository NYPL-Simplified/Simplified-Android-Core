package org.nypl.drm.core;

import java.util.Objects;

import java.io.File;

/**
 * Parameters for the Adobe Adept Connector.
 *
 * @see AdobeAdeptConnectorFactoryType
 */

public final class AdobeAdeptConnectorParameters
{
  private final String                         package_name;
  private final String                         package_version;
  private final AdobeAdeptResourceProviderType res;
  private final AdobeAdeptNetProviderType      net;
  private final String                         device_serial;
  private final String                         device_name;
  private final File                           app_storage;
  private final File                           xml_storage;
  private final File                           book_path;
  private final File                           temporary_dir;
  private final boolean                        debug_logging;

  /**
   * Construct connector parameters.
   *
   * @param in_package_name    The application package name
   * @param in_package_version The application package version
   * @param in_res             A resource provider
   * @param in_net             A net provider
   * @param in_device_serial   The serial number of the device
   * @param in_device_name     The name of the device
   * @param in_app_storage     The path to application storage
   * @param in_xml_storage     The path to XML storage
   * @param in_book_path       The path to fulfilled books
   * @param in_temporary_dir   A directory usable for temporary private file
   *                           storage (such as the per-application Android
   *                           external cache directory).
   * @param in_logging         {@code true} iff debug logging should initially
   *                           be enabled
   */

  public AdobeAdeptConnectorParameters(
    final String in_package_name,
    final String in_package_version,
    final AdobeAdeptResourceProviderType in_res,
    final AdobeAdeptNetProviderType in_net,
    final String in_device_serial,
    final String in_device_name,
    final File in_app_storage,
    final File in_xml_storage,
    final File in_book_path,
    final File in_temporary_dir,
    final boolean in_logging)
  {
    this.app_storage = Objects.requireNonNull(in_app_storage);
    this.package_name = Objects.requireNonNull(in_package_name);
    this.package_version = Objects.requireNonNull(in_package_version);
    this.res = Objects.requireNonNull(in_res);
    this.net = Objects.requireNonNull(in_net);
    this.device_serial = Objects.requireNonNull(in_device_serial);
    this.device_name = Objects.requireNonNull(in_device_name);
    this.xml_storage = Objects.requireNonNull(in_xml_storage);
    this.book_path = Objects.requireNonNull(in_book_path);
    this.temporary_dir = Objects.requireNonNull(in_temporary_dir);
    this.debug_logging = in_logging;
  }

  /**
   * @return {@code true} if debug logging should be left enabled after
   * initialization
   */

  public boolean isDebugLogging()
  {
    return this.debug_logging;
  }

  /**
   * @return The path to application storage
   */

  public File getApplicationStorage()
  {
    return this.app_storage;
  }

  /**
   * @return The path to fulfilled books
   */

  public File getBookPath()
  {
    return this.book_path;
  }

  /**
   * @return The name of the device
   */

  public String getDeviceName()
  {
    return this.device_name;
  }

  /**
   * @return The serial number of the device
   */

  public String getDeviceSerial()
  {
    return this.device_serial;
  }

  /**
   * @return The net provider
   */

  public AdobeAdeptNetProviderType getNetProvider()
  {
    return this.net;
  }

  /**
   * @return The application package name
   */

  public String getPackageName()
  {
    return this.package_name;
  }

  /**
   * @return The application package version
   */

  public String getPackageVersion()
  {
    return this.package_version;
  }

  /**
   * @return The resource provider
   */

  public AdobeAdeptResourceProviderType getResourceProvider()
  {
    return this.res;
  }

  /**
   * @return A directory usable for temporary private file storage (such as the
   * per-application Android external cache directory).
   */

  public File getTemporaryDirectory()
  {
    return this.temporary_dir;
  }

  /**
   * @return The path to XML storage
   */

  public File getXMLStorageDirectory()
  {
    return this.xml_storage;
  }
}
