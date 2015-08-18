package org.nypl.simplified.app.reader;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * The current page. A specific location in an EPUB is identified by an
 * <i>idref</i> and a <i>content CFI</i>. In some cases, the <i>content CFI</i>
 * may not be present.
 */

public final class ReaderBookLocation
  implements ReaderJSONSerializableType, Serializable
{
  private static final long serialVersionUID = 1L;
  private final OptionType<String> content_cfi;
  private final String             id_ref;

  private ReaderBookLocation(
    final String in_id_ref,
    final OptionType<String> in_content_cfi)
  {
    this.id_ref = NullCheck.notNull(in_id_ref);
    this.content_cfi = NullCheck.notNull(in_content_cfi);
  }

  /**
   * Construct a location from the given IDRef.
   *
   * @param i The IDRef
   *
   * @return A new location
   */

  public static ReaderBookLocation fromIDRef(
    final String i)
  {
    final OptionType<String> none = Option.none();
    return new ReaderBookLocation(NullCheck.notNull(i), none);
  }

  /**
   * Construct a location from the given JSON.
   *
   * @param o The JSON
   *
   * @return A new location
   *
   * @throws JSONException On JSON errors
   */

  public static ReaderBookLocation fromJSON(
    final JSONObject o)
    throws JSONException
  {
    NullCheck.notNull(o);

    final String in_id_ref = NullCheck.notNull(o.getString("idref"));
    final OptionType<String> in_content_cfi;
    if (o.has("contentCFI")) {
      in_content_cfi =
        Option.some(NullCheck.notNull(o.getString("contentCFI")));
    } else {
      in_content_cfi = Option.none();
    }

    return new ReaderBookLocation(in_id_ref, in_content_cfi);
  }

  /**
   * @return The content CFI, if any
   */

  public OptionType<String> getContentCFI()
  {
    return this.content_cfi;
  }

  /**
   * @return The IDRef
   */

  public String getIDRef()
  {
    return this.id_ref;
  }

  @Override public JSONObject toJSON()
    throws JSONException
  {
    final JSONObject json = new JSONObject();
    json.put("idref", this.id_ref);

    if (this.content_cfi.isSome()) {
      final Some<String> some = (Some<String>) this.content_cfi;
      json.put("contentCFI", some.get());
    }

    return json;
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder();
    b.append("[ReaderBookLocation ");
    b.append(this.content_cfi);
    b.append(" ");
    b.append(this.id_ref);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
