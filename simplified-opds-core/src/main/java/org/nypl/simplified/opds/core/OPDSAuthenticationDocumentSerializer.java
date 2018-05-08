package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Map;

/**
 * Default implementation of the {@link
 * OPDSAuthenticationDocumentSerializerType}
 * interface.
 */

public final class OPDSAuthenticationDocumentSerializer
  implements OPDSAuthenticationDocumentSerializerType
{
  private OPDSAuthenticationDocumentSerializer()
  {

  }

  /**
   * @return A document serializer
   */

  public static OPDSAuthenticationDocumentSerializerType get()
  {
    return new OPDSAuthenticationDocumentSerializer();
  }

  @Override
  public ObjectNode serializeDocument(final OPDSAuthenticationDocument e)
    throws OPDSSerializationException
  {
    NullCheck.notNull(e);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode je = jom.createObjectNode();

    je.put("id", e.getId());

    {
      final Map<String, String> labels = e.getLabels();
      if (labels.isEmpty() == false) {
        final ObjectNode jl = jom.createObjectNode();
        for (final String k : labels.keySet()) {
          final String v = NullCheck.notNull(labels.get(k));
          jl.put(k, v);
        }
        je.set("labels", jl);
      }
    }

    {
      final Map<String, OPDSLink> links = e.getLinks();
      if (links.isEmpty() == false) {
        final ObjectNode jl = jom.createObjectNode();
        for (final String k : links.keySet()) {
          final OPDSLink v = NullCheck.notNull(links.get(k));
          jl.set(k, this.serializeLink(jom, v));
        }
        je.set("links", jl);
      }
    }

    return null;
  }

  private ObjectNode serializeLink(
    final ObjectMapper jom,
    final OPDSLink v)
  {
    final ObjectNode n = jom.createObjectNode();
    n.put("href", v.getHref().toString());

    v.getHash().map_(
      new ProcedureType<String>()
      {
        @Override public void call(final String hash)
        {
          n.put("hash", hash);
        }
      });

    v.getLength().map_(
      new ProcedureType<BigInteger>()
      {
        @Override public void call(final BigInteger x)
        {
          n.set("length", new BigIntegerNode(x));
        }
      });

    v.getType().map_(
      new ProcedureType<String>()
      {
        @Override public void call(final String type)
        {
          n.put("type", type);
        }
      });

    return n;
  }

  @Override public void serializeToStream(
    final ObjectNode d,
    final OutputStream os)
    throws IOException
  {
    JSONSerializerUtilities.serializeObject(d, os);
  }
}
