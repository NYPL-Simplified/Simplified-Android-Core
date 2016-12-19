package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;

import java.net.URI;

/**
 * The type of mutable builders for {@link OPDSAcquisitionFeed} values.
 */

public interface OPDSAcquisitionFeedBuilderType
{
  /**
   * Add an entry to the feed being constructed.
   *
   * @param e The entry
   */

  void addEntry(
    OPDSAcquisitionFeedEntry e);

  /**
   * Set the URI of the privacy policy document for the feed, if any
   *
   * @param u The privacy policy URI, if any
   */

  void setPrivacyPolicyOption(OptionType<URI> u);

  /**
   * @return A feed consisting of all the values given so far
   */

  OPDSAcquisitionFeed build();

  /**
   * Set the URI of the next feed in a paginated feed
   *
   * @param next The next URI, if any
   */

  void setNextOption(
    OptionType<URI> next);

  /**
   * Set the URI of the search facilities for the given feed
   *
   * @param s The search URI, if any
   */

  void setSearchOption(
    OptionType<OPDSSearchLink> s);

  /**
   * Add the given facet.
   *
   * @param f The facet
   */

  void addFacet(
    OPDSFacet f);

  /**
   * Set the URI of the app about document for the feed, if any
   *
   * @param u The App About URI, if any
   */

  void setAboutOption(OptionType<URI> u);

  /**
   * Set the URI of the terms of service document for the feed, if any
   *
   * @param u The terms of service URI, if any
   */

  void setTermsOfServiceOption(OptionType<URI> u);

  /**
   * @param licensor
   */
  void setLisensor(OptionType<DRMLicensor> licensor);
}
