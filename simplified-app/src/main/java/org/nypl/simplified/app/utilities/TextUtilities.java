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
}
