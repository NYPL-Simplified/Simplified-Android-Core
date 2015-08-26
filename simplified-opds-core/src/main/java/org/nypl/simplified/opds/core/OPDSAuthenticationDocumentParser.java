package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The default implementation of the {@link
 * OPDSAuthenticationDocumentParserType}
 * interface.
 */

public final class OPDSAuthenticationDocumentParser
  implements OPDSAuthenticationDocumentParserType
{
  private OPDSAuthenticationDocumentParser()
  {

  }

  /**
   * @return A new authentication document parser.
   */

  public static OPDSAuthenticationDocumentParserType get()
  {
    return new OPDSAuthenticationDocumentParser();
  }

  @Override public OPDSAuthenticationDocument parseFromNode(final ObjectNode s)
    throws OPDSParseException
  {
    NullCheck.notNull(s);

    final String id = OPDSJSONParserUtilities.getString(s, "id");
    final String title = OPDSJSONParserUtilities.getString(s, "title");
    final OptionType<String> text_prompt =
      OPDSJSONParserUtilities.getStringOptional(s, "text");

    final List<URI> types = this.getTypes(s);
    final Map<String, OPDSLink> links = this.getLinks(s);
    final Map<String, String> labels = this.getLabels(s);

    return new OPDSAuthenticationDocument(
      id, types, title, text_prompt, links, labels);
  }

  private Map<String, String> getLabels(final ObjectNode s)
    throws OPDSParseException
  {
    final Map<String, String> m = new HashMap<String, String>();
    if (s.has("labels")) {
      OPDSJSONParserUtilities.getObjectOptional(s, "labels").mapPartial(
        new PartialFunctionType<ObjectNode, Unit, OPDSParseException>()
        {
          @Override public Unit call(final ObjectNode labels)
            throws OPDSParseException
          {
            final Iterator<String> field_iter = labels.fieldNames();
            while (field_iter.hasNext()) {
              final String field = field_iter.next();
              final String value =
                OPDSJSONParserUtilities.getString(labels, field);
              m.put(field, value);
            }
            return Unit.unit();
          }
        });
    }
    return m;
  }

  private Map<String, OPDSLink> getLinks(final ObjectNode s)
    throws OPDSParseException
  {
    final Map<String, OPDSLink> m = new HashMap<String, OPDSLink>();
    if (s.has("links")) {
      OPDSJSONParserUtilities.getObjectOptional(s, "links").mapPartial(
        new PartialFunctionType<ObjectNode, Unit, OPDSParseException>()
        {
          @Override public Unit call(final ObjectNode links)
            throws OPDSParseException
          {
            final Iterator<String> field_iter = links.fieldNames();
            while (field_iter.hasNext()) {
              final String field = field_iter.next();
              final OPDSLink value =
                OPDSAuthenticationDocumentParser.this.getLink(
                  OPDSJSONParserUtilities.getObject(links, field));
              m.put(field, value);
            }
            return Unit.unit();
          }
        });
    }
    return m;
  }

  private OPDSLink getLink(final ObjectNode link)
    throws OPDSParseException
  {
    final OptionType<String> hash =
      OPDSJSONParserUtilities.getStringOptional(link, "hash");
    final URI href = OPDSJSONParserUtilities.getURI(link, "href");
    final OptionType<String> title =
      OPDSJSONParserUtilities.getStringOptional(link, "title");
    final OptionType<String> type =
      OPDSJSONParserUtilities.getStringOptional(link, "type");
    final boolean templated =
      OPDSJSONParserUtilities.getBooleanDefault(link, "templated", false);
    final OptionType<BigInteger> length =
      OPDSJSONParserUtilities.getBigIntegerOptional(link, "length");
    return new OPDSLink(hash, href, title, type, templated, length);
  }

  private List<URI> getTypes(final ObjectNode s)
    throws OPDSParseException
  {
    try {
      final ArrayNode types = OPDSJSONParserUtilities.getArray(s, "type");
      final List<URI> r = new ArrayList<URI>(types.size());
      for (int index = 0; index < types.size(); ++index) {
        final JsonNode n = types.get(index);
        r.add(new URI(n.asText()));
      }
      return r;
    } catch (final URISyntaxException e) {
      throw new OPDSParseException(e);
    }
  }

  @Override
  public OPDSAuthenticationDocument parseFromStream(final InputStream s)
    throws OPDSParseException
  {
    NullCheck.notNull(s);

    try {
      final ObjectMapper jom = new ObjectMapper();
      return this.parseFromNode(
        OPDSJSONParserUtilities.checkObject(null, jom.readTree(s)));
    } catch (final JsonProcessingException e) {
      throw new OPDSParseException(e);
    } catch (final IOException e) {
      throw new OPDSParseException(e);
    }
  }
}
