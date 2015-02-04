package org.nypl.simplified.opds.core;

import java.util.Calendar;

import com.io7m.jnull.NullCheck;

/**
 * The type of OPDS acquisition feeds.
 */

@SuppressWarnings("synthetic-access") public final class OPDSAcquisitionFeed implements
  OPDSFeedType
{
  public static OPDSAcquisitionFeedBuilderType newBuilder(
    final String in_id,
    final Calendar in_updated,
    final String in_title)
  {
    NullCheck.notNull(in_id);
    NullCheck.notNull(in_updated);
    NullCheck.notNull(in_title);

    return new OPDSAcquisitionFeedBuilderType() {
      @Override public OPDSAcquisitionFeed build()
      {
        return new OPDSAcquisitionFeed(in_id, in_updated, in_title);
      }
    };
  }

  private final String   id;
  private final String   title;
  private final Calendar updated;

  private OPDSAcquisitionFeed(
    final String in_id,
    final Calendar in_updated,
    final String in_title)
  {
    this.id = NullCheck.notNull(in_id);
    this.updated = NullCheck.notNull(in_updated);
    this.title = NullCheck.notNull(in_title);
  }

  @Override public String getFeedID()
  {
    return this.id;
  }

  @Override public String getFeedTitle()
  {
    return this.title;
  }

  @Override public Calendar getFeedUpdated()
  {
    return this.updated;
  }

  @Override public <A, E extends Exception> A matchFeedType(
    final OPDSFeedMatcherType<A, E> m)
    throws E
  {
    return m.acquisition(this);
  }
}
