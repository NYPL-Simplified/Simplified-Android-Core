package org.nypl.simplified.opds.core;

import java.net.URI;

import com.io7m.jfunctional.OptionType;

/**
 * The type of mutable builders for {@link OPDSAcquisitionFeed} values.
 */

public interface OPDSAcquisitionFeedBuilderType
{
  /**
   * Add an entry to the feed being constructed.
   *
   * @param e
   *          The entry
   */

  void addEntry(
    OPDSAcquisitionFeedEntry e);

  /**
   * @return A feed consisting of all the values given so far
   */

  OPDSAcquisitionFeed build();

  /**
   * Set the URI of the next feed in a paginated feed
   *
   * @param next
   *          The next URI, if any
   */

  void setNextOption(
    OptionType<URI> next);

  /**
   * Set the URI of the search facilities for the given feed
   *
   * @param s
   *          The search URI, if any
   */

  void setSearchOption(
    OptionType<OPDSSearchLink> s);

  /**
   * Add the given facet.
   * 
   * @param f
   *          The facet
   */

  void addFacet(
    OPDSFacet f);
}
