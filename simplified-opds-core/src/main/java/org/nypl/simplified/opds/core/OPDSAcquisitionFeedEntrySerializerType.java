package org.nypl.simplified.opds.core;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <p>
 * The type of serializers that turn acquisition feed entries back into XML.
 * </p>
 * <p>
 * Implementations are required to be able to accept requests from any number
 * of threads simultaneously.
 * </p>
 */

public interface OPDSAcquisitionFeedEntrySerializerType
{
  Element serializeFeedEntryForDocument(
    Document d,
    OPDSAcquisitionFeedEntry e)
    throws OPDSFeedSerializationException;

  Document serializeFeedEntry(
    OPDSAcquisitionFeedEntry e)
    throws OPDSFeedSerializationException;
}
