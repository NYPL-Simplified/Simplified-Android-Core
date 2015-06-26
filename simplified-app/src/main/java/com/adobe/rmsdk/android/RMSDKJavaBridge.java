package com.adobe.rmsdk.android;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import android.content.Context;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * <p>
 * A custom RMSDK bridge that allows for overriding of the app ID in order to
 * avoid having to rename the package in the Android manifest when using a
 * development certificate.
 * </p>
 * <p>
 * The correct way to handle this would have been to have the programmer pass
 * in the app ID to the C++ API directly, rather than having platform-specific
 * code in the RMSDK that simply aborts the whole process when the app ID
 * doesn't match the certificate. This class is simply a workaround for that
 * broken behaviour.
 * </p>
 */

public class RMSDKJavaBridge
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(RMSDKJavaBridge.class);
  }

  private static final String PACKAGE_NAME;

  static {
    PACKAGE_NAME = "com.adobe.rmsdk.book2pngandroid";
  }

  public static String getPackageName()
  {
    RMSDKJavaBridge.LOG.debug(
      "package name requested: returning {}",
      RMSDKJavaBridge.PACKAGE_NAME);
    return RMSDKJavaBridge.PACKAGE_NAME;
  }

  /**
   * Initialize the RMSDK bridge. In this implementation, this function does
   * nothing and the context is ignored.
   *
   * @param c
   *          The app context
   * @return <tt>true</tt>
   */

  public static boolean initialize(
    final Context c)
  {
    NullCheck.notNull(c);
    return true;
  }

  private RMSDKJavaBridge()
  {
    throw new UnreachableCodeException();
  }
}
