package org.nypl.simplified.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.drm.core.AdobeAdeptConnectorFactory;
import org.nypl.drm.core.AdobeAdeptConnectorFactoryType;
import org.nypl.drm.core.AdobeAdeptContentFilterFactory;
import org.nypl.drm.core.AdobeAdeptContentFilterFactoryType;
import org.nypl.drm.core.AdobeAdeptContentFilterType;
import org.nypl.drm.core.AdobeAdeptExecutor;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeAdeptNetProvider;
import org.nypl.drm.core.AdobeAdeptNetProviderType;
import org.nypl.drm.core.AdobeAdeptResourceProvider;
import org.nypl.drm.core.AdobeAdeptResourceProviderType;
import org.nypl.drm.core.DRMException;
import org.nypl.drm.core.DRMUnsupportedException;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;

/**
 * Functions to initialize and control Adobe DRM.
 */

public final class AdobeDRMServices
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(AdobeDRMServices.class);
  }

  private AdobeDRMServices()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Retrieve or generate a serial number unique to this device.
   *
   * @param context The application context
   *
   * @return A unique device serial
   */

  public static String getDeviceSerial(final Context context)
  {
    NullCheck.notNull(context);

    final SharedPreferences settings =
      context.getSharedPreferences("adobe-drm-settings", 0);

    String serial = settings.getString("adobe-device-serial", null);
    if (serial == null || serial.length() != 64) {
      AdobeDRMServices.LOG.debug("generating new device serial");
      final SecureRandom rng = new SecureRandom();
      final byte[] bytes = new byte[32];
      rng.nextBytes(bytes);
      final StringBuilder sb = new StringBuilder();
      for (int index = 0; index < bytes.length; ++index) {
        sb.append(String.format("%02x", Byte.valueOf(bytes[index])));
      }
      serial = sb.toString();
    } else {
      AdobeDRMServices.LOG.debug("loaded existing device serial");
    }

    settings.edit().putString("adobe-device-serial", serial).apply();
    return serial;
  }

  /**
   * Attempt to load an Adobe DRM content filter implementation.
   *
   * @param context Application context
   *
   * @return A DRM content filter
   *
   * @throws DRMException If DRM is unavailable or cannot be initialized.
   */

  public static AdobeAdeptContentFilterType newAdobeContentFilter(
    final Context context)
    throws DRMException
  {
    NullCheck.notNull(context);

    final Logger log = AdobeDRMServices.LOG;

    final String device_name =
      String.format("%s/%s", Build.MANUFACTURER, Build.MODEL);
    final String device_serial = AdobeDRMServices.getDeviceSerial(context);
    log.debug("adobe device name:            {}", device_name);
    log.debug("adobe device serial:          {}", device_serial);

    final File app_storage = context.getFilesDir();
    final File xml_storage = context.getFilesDir();

    final File book_storage =
      new File(Simplified.getDiskDataDir(context), "adobe-books-tmp");
    final File temp_storage =
      new File(Simplified.getDiskDataDir(context), "adobe-tmp");

    log.debug("adobe app storage:            {}", app_storage);
    log.debug("adobe xml storage:            {}", xml_storage);
    log.debug("adobe temporary book storage: {}", book_storage);
    log.debug("adobe temporary storage:      {}", temp_storage);

    final Package p = Simplified.class.getPackage();
    final String package_name = p.getName();
    final String package_version = p.getImplementationVersion();
    final String agent = String.format("%s/%s", package_name, package_version);
    log.debug("adobe user agent:             {}", agent);

    final AdobeAdeptContentFilterFactoryType factory =
      AdobeAdeptContentFilterFactory.get();
    final AdobeAdeptResourceProviderType res = AdobeAdeptResourceProvider.get(
      AdobeDRMServices.getCertificateAsset(context.getAssets()));
    final AdobeAdeptNetProviderType net = AdobeAdeptNetProvider.get(agent);

    return factory.get(
      package_name,
      package_version,
      res,
      net,
      device_serial,
      device_name,
      app_storage,
      xml_storage,
      book_storage,
      temp_storage);
  }

  /**
   * Attempt to load an Adobe DRM implementation.
   *
   * @param context Application context
   *
   * @return A DRM implementation
   *
   * @throws DRMException If DRM is unavailable or cannot be initialized.
   */

  public static AdobeAdeptExecutorType newAdobeDRM(
    final Context context)
    throws DRMException
  {
    NullCheck.notNull(context);

    final Logger log = AdobeDRMServices.LOG;

    final String device_name =
      String.format("%s/%s", Build.MANUFACTURER, Build.MODEL);
    final String device_serial = AdobeDRMServices.getDeviceSerial(context);
    log.debug("adobe device name:            {}", device_name);
    log.debug("adobe device serial:          {}", device_serial);

    final File app_storage = context.getFilesDir();
    final File xml_storage = context.getFilesDir();

    final File book_storage =
      new File(Simplified.getDiskDataDir(context), "adobe-books-tmp");
    final File temp_storage =
      new File(Simplified.getDiskDataDir(context), "adobe-tmp");

    log.debug("adobe app storage:            {}", app_storage);
    log.debug("adobe xml storage:            {}", xml_storage);
    log.debug("adobe temporary book storage: {}", book_storage);
    log.debug("adobe temporary storage:      {}", temp_storage);

    final Package p = Simplified.class.getPackage();
    final String package_name = p.getName();
    final String package_version = p.getImplementationVersion();
    final String agent = String.format("%s/%s", package_name, package_version);
    log.debug("adobe user agent:             {}", agent);

    final AdobeAdeptConnectorFactoryType factory =
      AdobeAdeptConnectorFactory.get();
    final AdobeAdeptResourceProviderType res = AdobeAdeptResourceProvider.get(
      AdobeDRMServices.getCertificateAsset(context.getAssets()));
    final AdobeAdeptNetProviderType net = AdobeAdeptNetProvider.get(agent);

    try {
      return AdobeAdeptExecutor.newExecutor(
        factory,
        package_name,
        package_version,
        res,
        net,
        device_serial,
        device_name,
        app_storage,
        xml_storage,
        book_storage,
        temp_storage);
    } catch (final InterruptedException e) {
      throw new UnreachableCodeException();
    }
  }

  /**
   * Attempt to load an Adobe DRM implementation.
   *
   * @param context Application context
   *
   * @return A DRM implementation, if any are available
   */

  public static OptionType<AdobeAdeptExecutorType> newAdobeDRMOptional(
    final Context context)
  {
    try {
      return Option.some(
        AdobeDRMServices.newAdobeDRM(context));
    } catch (final DRMException e) {
      AdobeDRMServices.LOG.error("DRM is not supported: ", e);
      return Option.none();
    }
  }

  /**
   * Read the certificate from the Android assets.
   *
   * @param assets The assets
   *
   * @return A certificate
   *
   * @throws DRMUnsupportedException If the certificate is missing or
   *                                 inaccessible
   */

  private static byte[] getCertificateAsset(final AssetManager assets)
    throws DRMUnsupportedException
  {
    try {
      final InputStream is = assets.open("ReaderClientCert.sig");
      try {
        final ByteArrayOutputStream bao = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        while (true) {
          final int r = is.read(buffer);
          if (r == -1) {
            break;
          }
          bao.write(buffer, 0, r);
        }
        return bao.toByteArray();
      } finally {
        is.close();
      }
    } catch (final IOException e) {
      throw new DRMUnsupportedException(
        "ReaderClientCert.sig is unavailable", e);
    }
  }
}
