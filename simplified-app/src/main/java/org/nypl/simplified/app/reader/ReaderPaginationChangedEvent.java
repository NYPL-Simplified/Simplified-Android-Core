package org.nypl.simplified.app.reader;

import com.io7m.jnull.NullCheck;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Data parsed from a {@code readium:pagination-changed} event.
 */

@SuppressWarnings("synthetic-access")
public final class ReaderPaginationChangedEvent
  implements ReaderJSONSerializableType
{
  private final boolean        fixed_layout;
  private final List<OpenPage> open_pages;
  private final boolean        right_to_left;
  private final int            spine_item_count;

  /**
   * Construct an event.
   *
   * @param in_right_to_left    If pagination is right-to-left.
   * @param in_fixed_layout     If the layout is fixed
   * @param in_spine_item_count The number of spine items
   * @param in_open_pages       The list of open pages
   */

  public ReaderPaginationChangedEvent(
    final boolean in_right_to_left,
    final boolean in_fixed_layout,
    final int in_spine_item_count,
    final List<OpenPage> in_open_pages)
  {
    this.right_to_left = in_right_to_left;
    this.fixed_layout = in_fixed_layout;
    this.spine_item_count = in_spine_item_count;
    this.open_pages = NullCheck.notNull(in_open_pages);
  }

  /**
   * Deserialize an event.
   *
   * @param o The serialized event
   *
   * @return An event
   *
   * @throws JSONException On errors
   */

  public static ReaderPaginationChangedEvent fromJSON(
    final JSONObject o)
    throws JSONException
  {
    NullCheck.notNull(o);

    final boolean in_right_to_left = o.getBoolean("isRightToLeft");
    final boolean in_fixed_layout = o.getBoolean("isFixedLayout");
    final int in_spine_item_count = o.getInt("spineItemCount");
    final List<OpenPage> in_open_pages = new ArrayList<OpenPage>();

    final JSONArray oa = o.getJSONArray("openPages");
    for (int index = 0; index < oa.length(); ++index) {
      final JSONObject oi = NullCheck.notNull(oa.getJSONObject(index));
      in_open_pages.add(OpenPage.fromJSON(oi));
    }

    return new ReaderPaginationChangedEvent(
      in_right_to_left, in_fixed_layout, in_spine_item_count, in_open_pages);
  }

  /**
   * @return The list of open pages
   */

  public List<OpenPage> getOpenPages()
  {
    return this.open_pages;
  }

  /**
   * @return The fractional progress throughout the entire book, where {@code
   * 0.0} is the start of the book, and {@code 1.0} is the end.
   */

  public double getProgressFractional()
  {
    if (this.open_pages.size() < 1) {
      return 0.0;
    }

    final OpenPage page = NullCheck.notNull(this.open_pages.get(0));
    final double major_index = (double) page.spine_item_index;
    final double major_max = (double) this.spine_item_count;
    final double major = major_index / major_max;

    final double minor_index = (double) page.spine_item_page_index;
    final double minor_max = (double) page.spine_item_page_count;
    final double minor = minor_index / minor_max;

    return major + (minor * 0.1);
  }

  /**
   * @return The number of spine items
   */

  public int getSpineItemCount()
  {
    return this.spine_item_count;
  }

  /**
   * @return {@code true} if the layout is fixed
   */

  public boolean isFixedLayout()
  {
    return this.fixed_layout;
  }

  /**
   * @return {@code true} if the layout is right-to-left
   */

  public boolean isRightToLeft()
  {
    return this.right_to_left;
  }

  @Override public JSONObject toJSON()
    throws JSONException
  {
    final JSONObject o = new JSONObject();
    o.put("isRightToLeft", this.right_to_left);
    o.put("isFixedLayout", this.fixed_layout);
    o.put("spineItemCount", this.spine_item_count);

    for (final OpenPage p : this.open_pages) {
      o.accumulate("openPages", NullCheck.notNull(p).toJSON());
    }
    return o;
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder();
    b.append("[ReaderPaginationChangedEvent fixed_layout=");
    b.append(this.fixed_layout);
    b.append(" open_pages=");
    b.append(this.open_pages);
    b.append(" right_to_left=");
    b.append(this.right_to_left);
    b.append(" spine_item_count=");
    b.append(this.spine_item_count);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }

  /**
   * An open page.
   */

  public static final class OpenPage implements ReaderJSONSerializableType
  {
    private final String id_ref;
    private final int    spine_item_index;
    private final int    spine_item_page_count;
    private final int    spine_item_page_index;

    /**
     * Construct an open page.
     *
     * @param in_spine_item_page_index The spine item page index
     * @param in_spine_item_page_count The spine item page count
     * @param in_id_ref                The ID ref
     * @param in_spine_item_index      The spine item index
     */

    public OpenPage(
      final int in_spine_item_page_index,
      final int in_spine_item_page_count,
      final String in_id_ref,
      final int in_spine_item_index)
    {
      this.spine_item_page_index = in_spine_item_page_index;
      this.spine_item_page_count = in_spine_item_page_count;
      this.id_ref = NullCheck.notNull(in_id_ref);
      this.spine_item_index = in_spine_item_index;
    }

    /**
     * Deserialize an OpenPage request.
     *
     * @param o The serialized object
     *
     * @return A request
     *
     * @throws JSONException On errors
     */

    public static OpenPage fromJSON(
      final JSONObject o)
      throws JSONException
    {
      NullCheck.notNull(o);

      final int in_spine_item_page_index = o.getInt("spineItemPageIndex");
      final int in_spine_item_page_count = o.getInt("spineItemPageCount");
      final String in_id_ref = NullCheck.notNull(o.getString("idref"));
      final int in_spine_item_index = o.getInt("spineItemIndex");
      return new OpenPage(
        in_spine_item_page_index,
        in_spine_item_page_count,
        in_id_ref,
        in_spine_item_index);
    }

    /**
     * @return The ID ref
     */

    public String getIDRef()
    {
      return this.id_ref;
    }

    /**
     * @return The spine item index
     */

    public int getSpineItemIndex()
    {
      return this.spine_item_index;
    }

    /**
     * @return The spine item page count
     */

    public int getSpineItemPageCount()
    {
      return this.spine_item_page_count;
    }

    /**
     * @return The spine item page index
     */

    public int getSpineItemPageIndex()
    {
      return this.spine_item_page_index;
    }

    @Override public JSONObject toJSON()
      throws JSONException
    {
      final JSONObject o = new JSONObject();
      o.put("spineItemPageIndex", this.spine_item_page_index);
      o.put("spineItemPageCount", this.spine_item_page_count);
      o.put("idref", this.id_ref);
      o.put("spineItemIndex", this.spine_item_index);
      return o;
    }

    @Override public String toString()
    {
      final StringBuilder b = new StringBuilder();
      b.append("[OpenPage id_ref=");
      b.append(this.id_ref);
      b.append(" spine_item_index=");
      b.append(this.spine_item_index);
      b.append(" spine_item_page_count=");
      b.append(this.spine_item_page_count);
      b.append(" spine_item_page_index=");
      b.append(this.spine_item_page_index);
      b.append("]");
      return NullCheck.notNull(b.toString());
    }
  }
}
