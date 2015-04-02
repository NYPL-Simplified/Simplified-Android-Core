package org.nypl.simplified.app;

import com.io7m.jnull.NullCheck;

final class TextUtilities
{
  static String ellipsize(
    final String t,
    final int at)
  {
    if (t.length() > at) {
      return NullCheck.notNull(t.substring(0, at - 1) + "…");
    }
    return t;
  }
}
