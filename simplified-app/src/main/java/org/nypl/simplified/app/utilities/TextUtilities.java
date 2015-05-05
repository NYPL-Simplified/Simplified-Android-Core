package org.nypl.simplified.app.utilities;

import com.io7m.jnull.NullCheck;

/**
 * Simple string processing functions.
 */

public final class TextUtilities
{
  /**
   * If <tt>t</tt> is longer than <tt>at</tt> characters, truncate the string
   * to <tt>at - 1</tt> characters and end with "…".
   *
   * @param t
   *          The string
   * @param at
   *          The maximum length
   * @return An ellipsized string
   */

  public static String ellipsize(
    final String t,
    final int at)
  {
    if (t.length() > at) {
      return NullCheck.notNull(t.substring(0, at - 1) + "…");
    }
    return t;
  }

  /**
   * Brutally and overzealously attempt to remove one layer of quoting from a
   * given string. In other words "\"x\"" becomes "x".
   * 
   * @param t
   *          The string
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
