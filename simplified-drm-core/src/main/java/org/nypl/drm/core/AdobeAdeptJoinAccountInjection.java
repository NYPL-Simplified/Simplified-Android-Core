package org.nypl.drm.core;

import com.io7m.junreachable.UnreachableCodeException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Access to the injectable JavaScript used in the Join Accounts workflow.
 */

public final class AdobeAdeptJoinAccountInjection
{
  private AdobeAdeptJoinAccountInjection()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Retrieve the JavaScript that must be injected into the Adobe Join Accounts
   * workflow web form.
   *
   * @return A JavaScript expression
   */

  public static String getInjectedFormJavaScript()
  {
    final InputStream is =
      AdobeAdeptJoinAccountInjection.class.getResourceAsStream(
        "AdobeAdeptJoinAccountsInjected.js");

    try {
      final byte[] buffer = new byte[8192];
      final ByteArrayOutputStream bao = new ByteArrayOutputStream();
      while (true) {
        final int r = is.read(buffer);
        if (r == -1) {
          break;
        }
        bao.write(buffer, 0, r);
      }

      final String text = bao.toString("UTF-8");
      return text.replace('\n', ' ');
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    } finally {
      try {
        is.close();
      } catch (final IOException e) {
        throw new UnreachableCodeException(e);
      }
    }
  }
}
