package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;

/**
 * The default implementation of the {@link OPDSJSONSerializerType} interface.
 */

public final class OPDSJSONSerializer implements OPDSJSONSerializerType
{
  private OPDSJSONSerializer()
  {
    // Nothing
  }

  /**
   * @return A new JSON serializer
   */

  public static OPDSJSONSerializerType newSerializer()
  {
    return new OPDSJSONSerializer();
  }

  @Override public ObjectNode serializeAcquisition(
    final OPDSAcquisition a)
  {
    NullCheck.notNull(a);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode je = jom.createObjectNode();
    je.put("type", a.getType().toString());
    je.put("uri", a.getURI().toString());
    return je;
  }

  @Override public ObjectNode serializeAvailability(
    final OPDSAvailabilityType av)
  {
    NullCheck.notNull(av);

    final SimpleDateFormat fmt = OPDSRFC3339Formatter.newDateFormatter();
    final ObjectMapper jom = new ObjectMapper();
    return av.matchAvailability(
      new OPDSAvailabilityMatcherType<ObjectNode, UnreachableCodeException>()
      {
        @Override public ObjectNode onHeld(
          final OPDSAvailabilityHeld a)
        {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          oh.put("start_date", fmt.format(a.getStartDate().getTime()));
          oh.put("position", a.getPosition());
          o.set("held", oh);
          return o;
        }

        @Override public ObjectNode onHoldable(
          final OPDSAvailabilityHoldable a)
        {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          o.set("holdable", oh);
          return o;
        }

        @Override public ObjectNode onLoanable(
          final OPDSAvailabilityLoanable a)
        {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          o.set("loanable", oh);
          return o;
        }

        @Override public ObjectNode onLoaned(
          final OPDSAvailabilityLoaned a)
        {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          oh.put("start_date", fmt.format(a.getStartDate().getTime()));

          a.getEndDate().map(
            new FunctionType<Calendar, Unit>()
            {
              @Override public Unit call(
                final Calendar t)
              {
                oh.put("end_date", fmt.format(t.getTime()));
                return Unit.unit();
              }
            });

          o.set("loaned", oh);
          return o;
        }

        @Override public ObjectNode onOpenAccess(
          final OPDSAvailabilityOpenAccess a)
        {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          o.set("open_access", oh);
          return o;
        }
      });
  }

  @Override public ObjectNode serializeCategory(
    final OPDSCategory c)
  {
    NullCheck.notNull(c);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode je = jom.createObjectNode();
    je.put("scheme", c.getScheme());
    je.put("term", c.getTerm());
    return je;
  }

  @Override public ObjectNode serializeFeedEntry(
    final OPDSAcquisitionFeedEntry e)
    throws OPDSFeedSerializationException
  {
    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode je = jom.createObjectNode();
    final SimpleDateFormat fmt = OPDSRFC3339Formatter.newDateFormatter();

    {
      final ArrayNode ja = jom.createArrayNode();
      for (final String a : e.getAuthors()) {
        ja.add(a);
      }
      je.set("authors", ja);
    }

    {
      final ArrayNode ja = jom.createArrayNode();
      for (final OPDSAcquisition a : e.getAcquisitions()) {
        ja.add(this.serializeAcquisition(NullCheck.notNull(a)));
      }
      je.set("acquisitions", ja);
    }

    {
      je.set("availability", this.serializeAvailability(e.getAvailability()));
    }

    {
      final ArrayNode ja = jom.createArrayNode();
      for (final OPDSCategory c : e.getCategories()) {
        ja.add(this.serializeCategory(NullCheck.notNull(c)));
      }
      je.set("categories", ja);
    }

    e.getCover().map(
      new FunctionType<URI, Unit>()
      {
        @Override public Unit call(
          final URI u)
        {
          je.put("cover", u.toString());
          return Unit.unit();
        }
      });

    {
      final Set<Pair<String, URI>> eg = e.getGroups();
      final ArrayNode a = jom.createArrayNode();
      for (final Pair<String, URI> p : eg) {
        final ObjectNode o = jom.createObjectNode();
        o.put("name", p.getLeft());
        o.put("uri", p.getRight().toString());
        a.add(o);
      }
      je.set("groups", a);
    }

    je.put("id", e.getID());

    e.getPublished().map(
      new FunctionType<Calendar, Unit>()
      {
        @Override public Unit call(
          final Calendar c)
        {
          je.put("published", fmt.format(c.getTime()));
          return Unit.unit();
        }
      });

    e.getPublisher().map(
      new FunctionType<String, Unit>()
      {
        @Override public Unit call(
          final String s)
        {
          je.put("publisher", s);
          return Unit.unit();
        }
      });

    je.put("summary", e.getSummary());
    je.put("title", e.getTitle());

    e.getThumbnail().map(
      new FunctionType<URI, Unit>()
      {
        @Override public Unit call(
          final URI u)
        {
          je.put("thumbnail", u.toString());
          return Unit.unit();
        }
      });

    je.put("updated", fmt.format(e.getUpdated().getTime()));
    return NullCheck.notNull(je);
  }

  @Override public ObjectNode serializeFeed(
    final OPDSAcquisitionFeed e)
    throws OPDSFeedSerializationException
  {
    NullCheck.notNull(e);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode je = jom.createObjectNode();
    final SimpleDateFormat fmt = OPDSRFC3339Formatter.newDateFormatter();

    je.put("id", e.getFeedID());
    je.put("title", e.getFeedTitle());

    e.getFeedNext().map(
      new FunctionType<URI, Unit>()
      {
        @Override public Unit call(
          final URI next)
        {
          je.put("next", next.toString());
          return Unit.unit();
        }
      });

    {
      final ArrayNode a = jom.createArrayNode();
      for (final OPDSFacet k : e.getFeedFacetsOrder()) {
        final ObjectNode o = jom.createObjectNode();
        o.put("group", k.getGroup());
        o.put("active", k.isActive());
        o.put("title", k.getTitle());
        o.put("uri", k.getURI().toString());
        a.add(o);
      }
      je.set("facets", a);
    }

    {
      final ArrayNode a = jom.createArrayNode();
      for (final OPDSAcquisitionFeedEntry fe : e.getFeedEntries()) {
        a.add(this.serializeFeedEntry(fe));
      }
      je.set("entries", a);
    }

    e.getFeedSearchURI().map(
      new FunctionType<OPDSSearchLink, Unit>()
      {
        @Override public Unit call(
          final OPDSSearchLink s)
        {
          final ObjectNode os = jom.createObjectNode();
          os.put("type", s.getType());
          os.put("uri", s.getURI().toString());
          je.set("search", os);
          return Unit.unit();
        }
      });

    je.put("updated", fmt.format(e.getFeedUpdated().getTime()));
    je.put("uri", e.getFeedURI().toString());
    return NullCheck.notNull(je);
  }

  @Override public void serializeToStream(
    final ObjectNode d,
    final OutputStream os)
    throws IOException
  {
    NullCheck.notNull(d);
    NullCheck.notNull(os);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectWriter jw = jom.writerWithDefaultPrettyPrinter();
    jw.writeValue(os, d);
  }
}
