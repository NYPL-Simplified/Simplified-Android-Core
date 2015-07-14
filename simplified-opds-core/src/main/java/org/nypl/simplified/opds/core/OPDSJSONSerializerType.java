package org.nypl.simplified.opds.core;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * <p>
 * The type of serializers that produce simple JSON in a private format from
 * OPDS feeds.
 * </p>
 */

public interface OPDSJSONSerializerType
{
  ObjectNode serializeFeed(
    OPDSAcquisitionFeed e)
    throws OPDSFeedSerializationException;

  ObjectNode serializeFeedEntry(
    OPDSAcquisitionFeedEntry e)
    throws OPDSFeedSerializationException;

  ObjectNode serializeAvailability(
    OPDSAvailabilityType a);

  ObjectNode serializeAcquisition(
    OPDSAcquisition a);

  ObjectNode serializeCategory(
    OPDSCategory c);

  void serializeToStream(
    ObjectNode d,
    OutputStream os)
    throws IOException;
}
