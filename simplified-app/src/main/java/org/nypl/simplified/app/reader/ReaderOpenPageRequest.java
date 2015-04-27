package org.nypl.simplified.app.reader;

import org.json.JSONException;
import org.json.JSONObject;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * A request for a specific page in a book. This request is serialized to JSON
 * and passed to Readium's javascript API.
 */

@SuppressWarnings("synthetic-access") public final class ReaderOpenPageRequest
{
  private static final class FromContentAndSourceHref implements
    ReaderOpenPageRequestType
  {
    private static final long serialVersionUID = 1L;
    private final String      content;
    private final String      source_href;

    private FromContentAndSourceHref(
      final String in_content,
      final String in_source_href)
    {
      this.content = NullCheck.notNull(in_content);
      this.source_href = NullCheck.notNull(in_source_href);
    }

    @Override public JSONObject toJSON()
      throws JSONException
    {
      final JSONObject json = new JSONObject();
      json.put("contentRefUrl", this.content);
      json.put("sourceFileHref", this.source_href);
      return json;
    }
  }

  private static final class FromElementCFIAndIDRef implements
    ReaderOpenPageRequestType
  {
    private static final long        serialVersionUID = 1L;
    private final OptionType<String> element_cfi;
    private final String             id_ref;

    private FromElementCFIAndIDRef(
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

  public static ReaderOpenPageRequestType fromContentAndSourceHref(
    final String in_content,
    final String in_source_href)
  {
    return new FromContentAndSourceHref(in_content, in_source_href);
  }

  public static ReaderOpenPageRequestType fromElementCFIAndIDRef(
    final String in_id_ref,
    final OptionType<String> in_element_cfi)
  {
    return new FromElementCFIAndIDRef(in_id_ref, in_element_cfi);
  }

  private ReaderOpenPageRequest()
  {
    throw new UnreachableCodeException();
  }
}
