package org.nypl.simplified.viewer.epub.readium1;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.simplified.reader.api.ReaderFontSelection;

/**
 * The current viewer settings.
 */

public final class ReaderReadiumViewerSettings
  implements ReaderJSONSerializableType
{
  private final int                 column_gap;
  private final ReaderFontSelection font_selection;
  private final int                 font_size;
  private final ScrollMode          scroll_mode;
  private final SyntheticSpreadMode synthetic_spread;

  /**
   * Construct new viewer settings.
   *
   * @param in_synthetic_spread The synthetic spread mode
   * @param in_scroll_mode      The scroll mode
   * @param in_font_selection   The font selection
   * @param in_font_size        The font size
   * @param in_column_gap       The column gap
   */

  public ReaderReadiumViewerSettings(
    final SyntheticSpreadMode in_synthetic_spread,
    final ScrollMode in_scroll_mode,
    final ReaderFontSelection in_font_selection,
    final int in_font_size,
    final int in_column_gap)
  {
    this.synthetic_spread = NullCheck.notNull(in_synthetic_spread);
    this.scroll_mode = NullCheck.notNull(in_scroll_mode);
    this.font_selection = in_font_selection;
    this.font_size = in_font_size;
    this.column_gap = in_column_gap;
  }

  /**
   * @return The current column gap
   */

  public int getColumnGap()
  {
    return this.column_gap;
  }

  /**
   * @return The font selection
   */

  public ReaderFontSelection getFontSelection()
  {
    return this.font_selection;
  }

  /**
   * @return The font size
   */

  public int getFontSize()
  {
    return this.font_size;
  }

  /**
   * @return The scroll mode
   */

  public ScrollMode getScrollMode()
  {
    return this.scroll_mode;
  }

  /**
   * @return The synthetic spread mode
   */

  public SyntheticSpreadMode getSyntheticSpreadMode()
  {
    return this.synthetic_spread;
  }

  /**
   * @return The Readium index number of the font.
   */
  private static int readiumFontIndex(
    final ReaderFontSelection selection) {
    switch (selection) {
      // These index values must correspond to the ordering of the fonts passed to the ReaderView
      // constructor in host_app_feedback.js.
      case READER_FONT_SERIF:
        return 1;
      case READER_FONT_SANS_SERIF:
        return 2;
      case READER_FONT_OPEN_DYSLEXIC:
        return 3;
    }
    throw new UnreachableCodeException();
  }

  @Override public JSONObject toJSON()
    throws JSONException
  {
    final JSONObject json = new JSONObject();
    json.put("syntheticSpread", this.synthetic_spread.getValue());
    json.put("scroll", this.scroll_mode.getValue());
    json.put("fontSelection", readiumFontIndex(this.font_selection));
    json.put("fontSize", this.font_size);
    json.put("columnGap", this.column_gap);
    return json;
  }

  /**
   * The type of reader scroll modes.
   */

  public enum ScrollMode
  {
    /**
     * Auto scrolling.
     */

    AUTO("auto"),

    /**
     * Continuous scrolling.
     */

    CONTINUOUS("scroll-continuous"),

    /**
     * "Document" scrolling.
     */

    DOCUMENT("scroll-doc");

    private final String value;

    ScrollMode(
      final String in_value)
    {
      this.value = NullCheck.notNull(in_value);
    }

    /**
     * @return The string value for the element
     */

    public String getValue()
    {
      return this.value;
    }
  }

  /**
   * The type of synthetic spread modes.
   */

  public enum SyntheticSpreadMode
  {
    /**
     * Auto spread mode.
     */

    AUTO("auto"),

    /**
     * Double spread mode.
     */

    DOUBLE("double"),

    /**
     * Single spread mode.
     */

    SINGLE("single");

    private final String value;

    SyntheticSpreadMode(
      final String in_value)
    {
      this.value = NullCheck.notNull(in_value);
    }

    /**
     * @return The string value for the element.
     */

    public String getValue()
    {
      return this.value;
    }
  }
}
