package org.nypl.simplified.app.reader;

import android.graphics.Color;

/**
 * The selected color scheme for the reader.
 */

public enum ReaderColorScheme
{
  /**
   * Black text on a beige backdrop.
   */

  SCHEME_BLACK_ON_BEIGE(Color.argb(0xff, 242, 228, 203), Color.BLACK),

  /**
   * Black text on a white backdrop.
   */

  SCHEME_BLACK_ON_WHITE(Color.WHITE, Color.BLACK),

  /**
   * White text on a black backdrop.
   */

  SCHEME_WHITE_ON_BLACK(Color.BLACK, Color.WHITE)

  ;

  private int back;
  private int fore;

  private ReaderColorScheme(
    final int bg,
    final int fg)
  {
    this.back = bg;
    this.fore = fg;
  }

  public int getBackgroundColor()
  {
    return this.back;
  }

  public int getForegroundColor()
  {
    return this.fore;
  }
}
