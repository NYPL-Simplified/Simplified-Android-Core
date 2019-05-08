package org.nypl.simplified.app.reader;

import android.graphics.Color;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.reader.api.ReaderColorScheme;

/**
 * Color schemes.
 */

public final class ReaderColorSchemes {

  private ReaderColorSchemes()
  {

  }

  public static int background(final ReaderColorScheme scheme)
  {
    NullCheck.notNull(scheme, "Scheme");

    switch (scheme) {
      case SCHEME_BLACK_ON_BEIGE:
        return Color.argb(0xff, 242, 228, 203);
      case SCHEME_BLACK_ON_WHITE:
        return Color.WHITE;
      case SCHEME_WHITE_ON_BLACK:
        return Color.BLACK;
    }

    throw new UnreachableCodeException();
  }

  public static int foreground(final ReaderColorScheme scheme)
  {
    NullCheck.notNull(scheme, "Scheme");

    switch (scheme) {
      case SCHEME_BLACK_ON_BEIGE:
        return Color.BLACK;
      case SCHEME_BLACK_ON_WHITE:
        return Color.BLACK;
      case SCHEME_WHITE_ON_BLACK:
        return Color.WHITE;
    }

    throw new UnreachableCodeException();
  }
}
