package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of mutable builders for {@link OPDSNavigationFeed} values.
 */

public interface OPDSNavigationFeedBuilderType
{
  /**
   * Add an entry to the feed being constructed.
   *
   * @param e
   *          The entry
   */

  void addEntry(
    OPDSNavigationFeedEntry e);

  /**
   * @return A feed consisting of all the values given so far
   */

  OPDSNavigationFeed build();

  /**
   * Set the URI of the search facilities for the given feed
   *
   * @param s
   *          The search URI, if any
   */

  void setSearchOption(
    OptionType<OPDSSearchLink> s);
}
