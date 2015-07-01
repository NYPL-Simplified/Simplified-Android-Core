package org.nypl.simplified.app.drm;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Functions to load and use the Adobe Adept Connector (the DRM component of
 * the RMSDK).
 */

final class AdobeAdeptConnector implements AdobeAdeptConnectorType
{
  private static final Logger                  LOG;
  private static @Nullable AdobeAdeptConnector INSTANCE;

  static {
    LOG = LogUtilities.getLog(AdobeAdeptConnector.class);
  }

  private static native boolean nyadInitialize(
    final AdobeAdeptResourceProviderType res,
    final AdobeAdeptDRMClientType client,
    final byte[] device_serial,
    final byte[] device_name,
    final byte[] app_storage,
    final byte[] xml_storage,
    final byte[] book_path);

  private static native int nyadAuthorizeDevice(
    final byte[] vendor,
    final byte[] user,
    final byte[] password);

  public static AdobeAdeptConnectorType openConnector(
    final AdobeAdeptResourceProviderType res,
    final AdobeAdeptDRMClientType client,
    final String device_serial,
    final String device_name,
    final File app_storage,
    final File xml_storage,
    final File book_path)
    throws DRMUnsupportedException
  {
    NullCheck.notNull(res);
    NullCheck.notNull(client);
    NullCheck.notNull(device_serial);
    NullCheck.notNull(device_name);
    NullCheck.notNull(app_storage);
    NullCheck.notNull(xml_storage);
    NullCheck.notNull(book_path);

    if (AdobeAdeptConnector.INSTANCE != null) {
      AdobeAdeptConnector.LOG.debug("reusing instance");
      return NullCheck.notNull(AdobeAdeptConnector.INSTANCE);
    }

    try {
      AdobeAdeptConnector.LOG
        .debug("attempting to load nypl_adobe DRM library");
      System.loadLibrary("nypl_adobe");
      final AdobeAdeptConnector p = new AdobeAdeptConnector();

      AdobeAdeptConnector.LOG.debug("calling nyadInitialize");
      if (AdobeAdeptConnector.nyadInitialize(
        res,
        client,
        AdobeAdeptConnector.getUTF8Bytes(device_serial),
        AdobeAdeptConnector.getUTF8Bytes(device_name),
        AdobeAdeptConnector.getUTF8Bytes(app_storage.getAbsolutePath()),
        AdobeAdeptConnector.getUTF8Bytes(xml_storage.getAbsolutePath()),
        AdobeAdeptConnector.getUTF8Bytes(book_path.getAbsolutePath()))) {
        AdobeAdeptConnector.INSTANCE = p;
        return p;
      }

      throw new DRMUnsupportedException("DRM library failed to initialize");
    } catch (final UnsatisfiedLinkError e) {
      throw new DRMUnsupportedException(e);
    }
  }

  private static byte[] getUTF8Bytes(
    final String s)
  {
    try {
      return NullCheck.notNull(s.getBytes("UTF-8"));
    } catch (final UnsupportedEncodingException e) {
      throw new UnreachableCodeException(e);
    }
  }

  private AdobeAdeptConnector()
  {

  }

  @Override public void authorizeDevice(
    final String vendor,
    final String user,
    final String password)
  {
    try {
      AdobeAdeptConnector.nyadAuthorizeDevice(
        vendor.getBytes("UTF-8"),
        user.getBytes("UTF-8"),
        password.getBytes("UTF-8"));
    } catch (final UnsupportedEncodingException e) {
      throw new UnreachableCodeException(e);
    }
  }
}
