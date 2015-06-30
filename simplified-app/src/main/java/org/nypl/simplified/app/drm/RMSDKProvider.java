package org.nypl.simplified.app.drm;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Functions to load and use the DRM connector from the Adobe RMSDK.
 */

final class RMSDKProvider
{
  private static final Logger            LOG;
  private static @Nullable RMSDKProvider INSTANCE;

  static {
    LOG = LogUtilities.getLog(RMSDKProvider.class);
  }

  private static native void nyadInitialize(
    final RMSDKResourceProviderType res,
    final byte[] device_serial,
    final byte[] device_name,
    final byte[] app_storage,
    final byte[] xml_storage,
    final byte[] book_path);

  public static RMSDKProvider openProvider(
    final RMSDKResourceProviderType res,
    final String device_serial,
    final String device_name,
    final File app_storage,
    final File xml_storage,
    final File book_path)
    throws DRMUnsupportedException
  {
    NullCheck.notNull(res);
    NullCheck.notNull(device_serial);
    NullCheck.notNull(device_name);
    NullCheck.notNull(app_storage);
    NullCheck.notNull(xml_storage);
    NullCheck.notNull(book_path);

    if (RMSDKProvider.INSTANCE != null) {
      RMSDKProvider.LOG.debug("reusing instance");
      return NullCheck.notNull(RMSDKProvider.INSTANCE);
    }

    try {
      RMSDKProvider.LOG.debug("attempting to load nypl_adobe DRM library");
      System.loadLibrary("nypl_adobe");
      final RMSDKProvider p = new RMSDKProvider();
      RMSDKProvider.LOG.debug("calling nyadInitialize");
      RMSDKProvider.nyadInitialize(
        res,
        RMSDKProvider.getUTF8Bytes(device_serial),
        RMSDKProvider.getUTF8Bytes(device_name),
        RMSDKProvider.getUTF8Bytes(app_storage.getAbsolutePath()),
        RMSDKProvider.getUTF8Bytes(xml_storage.getAbsolutePath()),
        RMSDKProvider.getUTF8Bytes(book_path.getAbsolutePath()));
      RMSDKProvider.INSTANCE = p;
      return p;
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

  private RMSDKProvider()
  {

  }
}
