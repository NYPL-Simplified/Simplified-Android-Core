package org.nypl.simplified.app.reader;

import org.json.JSONException;
import org.json.JSONObject;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

/**
 * A request for a specific page in a book. This request is serialized to JSON
 * and passed to Readium's javascript API.
 */

public final class ReaderOpenPageRequest implements
  ReaderJSONSerializableType
{
  private final OptionType<String> element_cfi;
  private final String             id_ref;

  private ReaderOpenPageRequest(
    final String in_id_ref,
    final OptionType<String> in_element_cfi)
  {
    this.id_ref = NullCheck.notNull(in_id_ref);
    this.element_cfi = NullCheck.notNull(in_element_cfi);
  }

  @Override public JSONObject toJSON()
    throws JSONException
  {
    final JSONObject json = new JSONObject();
    json.put("idref", this.id_ref);

    if (this.element_cfi.isSome()) {
      final Some<String> some = (Some<String>) this.element_cfi;
      json.put("elementCfi", some.get());
    }

    return json;
  }
}
