package org.nypl.simplified.app.reader;

import org.json.JSONException;
import org.json.JSONObject;

import com.io7m.jnull.NullCheck;

public final class ReaderViewerSettings implements ReaderJSONSerializableType
{
  public enum ScrollMode
  {
    AUTO("auto"),
    CONTINUOUS("scroll-continuous"),
    DOCUMENT("scroll-doc");

    private final String value;

    private ScrollMode(
      final String in_value)
    {
      this.value = NullCheck.notNull(in_value);
    }

    public String getValue()
    {
      return this.value;
    }
  }

  public enum SyntheticSpreadMode
  {
    AUTO("auto"),
    DOUBLE("double"),
    SINGLE("single");

    private final String value;

    private SyntheticSpreadMode(
      final String in_value)
    {
      this.value = NullCheck.notNull(in_value);
    }

    public String getValue()
    {
      return this.value;
    }
  }

  private final int                 column_gap;
  private final int                 font_size;
  private final ScrollMode          scroll_mode;
  private final SyntheticSpreadMode synthetic_spread;

  public ReaderViewerSettings(
    final SyntheticSpreadMode in_synthetic_spread,
    final ScrollMode in_scroll_mode,
    final int in_font_size,
    final int in_column_gap)
  {
    this.synthetic_spread = NullCheck.notNull(in_synthetic_spread);
    this.scroll_mode = NullCheck.notNull(in_scroll_mode);
    this.font_size = in_font_size;
    this.column_gap = in_column_gap;
  }

  public int getColumnGap()
  {
    return this.column_gap;
  }

  public int getFontSize()
  {
    return this.font_size;
  }

  public ScrollMode getScrollMode()
  {
    return this.scroll_mode;
  }

  public SyntheticSpreadMode getSyntheticSpreadMode()
  {
    return this.synthetic_spread;
  }

  @Override public JSONObject toJSON()
    throws JSONException
  {
    final JSONObject json = new JSONObject();
    json.put("syntheticSpread", this.synthetic_spread.getValue());
    json.put("scroll", this.scroll_mode.getValue());
    json.put("fontSize", this.font_size);
    json.put("columnGap", this.column_gap);
    return json;
  }
}
