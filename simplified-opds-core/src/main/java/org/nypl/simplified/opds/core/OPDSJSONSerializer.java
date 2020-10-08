package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * The default implementation of the {@link OPDSJSONSerializerType} interface.
 */

public final class OPDSJSONSerializer implements OPDSJSONSerializerType {
  private OPDSJSONSerializer() {
    // Nothing
  }

  /**
   * @return A new JSON serializer
   */

  public static OPDSJSONSerializerType newSerializer() {
    return new OPDSJSONSerializer();
  }

  @Override
  public ObjectNode serializeAcquisition(
    final OPDSAcquisition a)
    throws OPDSSerializationException {
    NullCheck.notNull(a, "Acquisition");

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode node = jom.createObjectNode();
    node.put("type", a.getRelation().toString());
    node.put("uri", a.getUri().toString());
    node.put("content_type", a.getType().getFullType());
    node.set("indirect_acquisitions", serializeIndirectAcquisitions(a.getIndirectAcquisitions()));
    return node;
  }

  @Override
  public ArrayNode serializeIndirectAcquisitions(
    final List<OPDSIndirectAcquisition> indirects)
    throws OPDSSerializationException {
    NullCheck.notNull(indirects, "Indirects");

    final ObjectMapper jom = new ObjectMapper();
    final ArrayNode node = jom.createArrayNode();

    for (OPDSIndirectAcquisition indirect : indirects) {
      node.add(serializeIndirectAcquisition(indirect));
    }
    return node;
  }

  @Override
  public ObjectNode serializeIndirectAcquisition(
    final OPDSIndirectAcquisition indirect)
    throws OPDSSerializationException {
    NullCheck.notNull(indirect, "Indirect");

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode node = jom.createObjectNode();

    node.put("type", indirect.getType().getFullType());
    node.set("indirect_acquisitions",
      serializeIndirectAcquisitions(indirect.getIndirectAcquisitions()));
    return node;
  }

  @Override
  public ObjectNode serializeAvailability(
    final OPDSAvailabilityType av) {
    NullCheck.notNull(av);

    final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    final ObjectMapper jom = new ObjectMapper();
    return av.matchAvailability(
      new OPDSAvailabilityMatcherType<ObjectNode, UnreachableCodeException>() {
        @Override
        public ObjectNode onHeldReady(final OPDSAvailabilityHeldReady a) {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          a.getEndDate().map(t -> {
            oh.put("end_date", fmt.print(t));
            return Unit.unit();
          });
          a.getRevoke().map(uri -> {
            oh.put("revoke", uri.toString());
            return Unit.unit();
          });
          o.set("held_ready", oh);
          return o;
        }

        @Override
        public ObjectNode onHeld(final OPDSAvailabilityHeld a) {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          a.getStartDate().map(t -> {
            oh.put("start_date", fmt.print(t));
            return Unit.unit();
          });
          a.getPosition().map(x -> {
            oh.put("position", x);
            return Unit.unit();
          });
          a.getRevoke().map(uri -> {
            oh.put("revoke", uri.toString());
            return Unit.unit();
          });

          o.set("held", oh);
          return o;
        }

        @Override
        public ObjectNode onHoldable(final OPDSAvailabilityHoldable a) {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          o.set("holdable", oh);
          return o;
        }

        @Override
        public ObjectNode onLoanable(final OPDSAvailabilityLoanable a) {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          o.set("loanable", oh);
          return o;
        }

        @Override
        public ObjectNode onLoaned(final OPDSAvailabilityLoaned a) {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          a.getStartDate().map(t -> {
            oh.put("start_date", fmt.print(t));
            return Unit.unit();
          });
          a.getEndDate().map(t -> {
            oh.put("end_date", fmt.print(t));
            return Unit.unit();
          });
          a.getRevoke().map(uri -> {
            oh.put("revoke", uri.toString());
            return Unit.unit();
          });

          o.set("loaned", oh);
          return o;
        }

        @Override
        public ObjectNode onOpenAccess(final OPDSAvailabilityOpenAccess a) {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          a.getRevoke().map(
            uri -> {
              oh.put("revoke", uri.toString());
              return Unit.unit();
            });
          o.set("open_access", oh);
          return o;
        }

        @Override
        public ObjectNode onRevoked(final OPDSAvailabilityRevoked a) {
          final ObjectNode o = jom.createObjectNode();
          final ObjectNode oh = jom.createObjectNode();
          oh.put("revoke", a.getRevoke().toString());
          o.set("revoked", oh);
          return o;
        }
      });
  }

  @Override
  public ObjectNode serializeCategory(
    final OPDSCategory c) {
    NullCheck.notNull(c);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode je = jom.createObjectNode();
    je.put("scheme", c.getScheme());
    je.put("term", c.getTerm());

    final OptionType<String> label_opt = c.getLabel();
    if (label_opt.isSome()) {
      final Some<String> some = (Some<String>) label_opt;
      je.put("label", some.get());
    }

    return je;
  }

  @Override
  public ObjectNode serializeLicensor(final DRMLicensor l) {
    NullCheck.notNull(l);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode je = jom.createObjectNode();
    je.put("vendor", l.getVendor());
    je.put("clientToken", l.getClientToken());

    if (l.getDeviceManager().isSome()) {
      je.put("deviceManager", ((Some<String>) l.getDeviceManager()).get());
    }

    return je;
  }

  @Override
  public ObjectNode serializeFeedEntry(
    final OPDSAcquisitionFeedEntry e)
    throws OPDSSerializationException {
    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode je = jom.createObjectNode();
    final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

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
      if (e.getLicensor().isSome()) {
        je.set("licensor", this.serializeLicensor(((Some<DRMLicensor>) e.getLicensor()).get()));
      }
    }

    {
      final ArrayNode ja = jom.createArrayNode();
      for (final OPDSCategory c : e.getCategories()) {
        ja.add(this.serializeCategory(NullCheck.notNull(c)));
      }
      je.set("categories", ja);
    }

    e.getCover().map(u -> {
      je.put("cover", u.toString());
      return Unit.unit();
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

    e.getPublished().map(c -> {
      je.put("published", fmt.print(c));
      return Unit.unit();
    });

    e.getPublisher().map(s -> {
      je.put("publisher", s);
      return Unit.unit();
    });

    je.put("distribution", e.getDistribution());
    je.put("summary", e.getSummary());
    je.put("title", e.getTitle());

    e.getThumbnail().map(u -> {
      je.put("thumbnail", u.toString());
      return Unit.unit();
    });


    e.getAlternate().map(u -> {
      je.put("alternate", u.toString());
      je.put("analytics", u.toString().replace("/works/", "/analytics/"));
      return Unit.unit();
    });

    e.getAnnotations().map(
      u -> {
        je.put("annotations", u.toString());
        return Unit.unit();
      });

    je.put("updated", fmt.print(e.getUpdated()));
    return NullCheck.notNull(je);
  }

  @Override
  public ObjectNode serializeFeed(
    final OPDSAcquisitionFeed e)
    throws OPDSSerializationException {
    NullCheck.notNull(e);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode je = jom.createObjectNode();
    final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

    je.put("id", e.getFeedID());
    je.put("title", e.getFeedTitle());

    e.getFeedNext().map(next -> {
      je.put("next", next.toString());
      return Unit.unit();
    });

    {
      final ArrayNode a = jom.createArrayNode();
      for (final OPDSFacet k : e.getFeedFacetsOrder()) {
        final ObjectNode o = jom.createObjectNode();
        o.put("group", k.getGroup());
        o.put("active", k.isActive());
        o.put("title", k.getTitle());
        o.put("uri", k.getUri().toString());
        k.getGroupType().map_(type -> o.put("group_type", type));
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
      s -> {
        final ObjectNode os = jom.createObjectNode();
        os.put("type", s.getType());
        os.put("uri", s.getURI().toString());
        je.set("search", os);
        return Unit.unit();
      });

    je.put("updated", fmt.print(e.getFeedUpdated()));
    je.put("uri", e.getFeedURI().toString());
    return NullCheck.notNull(je);
  }

  @Override
  public void serializeToStream(
    final ObjectNode d,
    final OutputStream os)
    throws IOException {
    JSONSerializerUtilities.serialize(d, os);
  }
}
