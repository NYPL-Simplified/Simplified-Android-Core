package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p> The type of serializers that produce simple JSON in a private format from
 * OPDS feeds. </p>
 */

public interface OPDSJSONSerializerType
{
  /**
   * Serialize the given feed to JSON.
   *
   * @param e The feed
   *
   * @return JSON
   *
   * @throws OPDSFeedSerializationException On serialization errors
   */

  ObjectNode serializeFeed(
    OPDSAcquisitionFeed e)
    throws OPDSFeedSerializationException;

  /**
   * Serialize the given feed entry to JSON.
   *
   * @param e The feed entry
   *
   * @return JSON
   *
   * @throws OPDSFeedSerializationException On serialization errors
   */

  ObjectNode serializeFeedEntry(
    OPDSAcquisitionFeedEntry e)
    throws OPDSFeedSerializationException;

  /**
   * Serialize the given availability type to JSON.
   *
   * @param a The availability type
   *
   * @return JSON
   */

  ObjectNode serializeAvailability(
    OPDSAvailabilityType a);

  /**
   * Serialize the given acquisition to JSON.
   *
   * @param a The acquisition
   *
   * @return JSON
   */

  ObjectNode serializeAcquisition(
    OPDSAcquisition a);

  /**
   * Serialize the given category to JSON.
   *
   * @param c The category
   *
   * @return JSON
   */

  ObjectNode serializeCategory(
    OPDSCategory c);

  /**
   * Serialize the given JSON to the given output stream.
   *
   * @param d  The JSON
   * @param os The output stream
   *
   * @throws IOException On I/O errors
   */

  void serializeToStream(
    ObjectNode d,
    OutputStream os)
    throws IOException;
}
