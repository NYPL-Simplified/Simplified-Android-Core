package org.nypl.simplified.app.reader;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.io7m.jnull.NullCheck;

/**
 * Data parsed from a <tt>readium:pagination-changed</tt> event.
 */

public final class ReaderPaginationChangedEvent implements
  ReaderJSONSerializableType
{
  public static final class OpenPage implements ReaderJSONSerializableType
  {
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

    private final String id_ref;
    private final int    spine_item_index;
    private final int    spine_item_page_count;

    private final int    spine_item_page_index;

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

    public String getIDRef()
    {
      return this.id_ref;
    }

    public int getSpineItemIndex()
    {
      return this.spine_item_index;
    }

    public int getSpineItemPageCount()
    {
      return this.spine_item_page_count;
    }

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
  }

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
      in_right_to_left,
      in_fixed_layout,
      in_spine_item_count,
      in_open_pages);
  }

  private final boolean        fixed_layout;
  private final List<OpenPage> open_pages;
  private final boolean        right_to_left;
  private final int            spine_item_count;

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

  public List<OpenPage> getOpenPages()
  {
    return this.open_pages;
  }

  public int getSpineItemCount()
  {
    return this.spine_item_count;
  }

  public boolean isFixedLayout()
  {
    return this.fixed_layout;
  }

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
}
