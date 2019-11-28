package org.nypl.simplified.viewer.epub.readium1;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Simple string processing functions.
 */

public final class TextUtilities
{
  private TextUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Brutally and overzealously attempt to remove one layer of quoting from a
   * given string. In other words "\"x\"" becomes "x".
   *
   * @param t The string
   *
   * @return The unquoted string
   */

  public static String unquote(
    final String t)
  {
    final String t0 = NullCheck.notNull(t);
    final String t1 = t0.substring(1, t0.length() - 1);
    final String t2 = t1.replaceAll("\\\\\"", "\"");
    return NullCheck.notNull(t2);
  }
}
