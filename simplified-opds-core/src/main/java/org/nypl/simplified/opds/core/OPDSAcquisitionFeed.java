package org.nypl.simplified.opds.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.io7m.jnull.NullCheck;

/**
 * The type of OPDS acquisition feeds.
 */

@SuppressWarnings("synthetic-access") public final class OPDSAcquisitionFeed implements
  OPDSFeedType
{
  private static final long serialVersionUID = -7962463871020194252L;

  private static final class Builder implements
    OPDSAcquisitionFeedBuilderType
  {
    private final List<OPDSAcquisitionFeedEntry> entries;
    private final String                         id;
    private final String                         title;
    private final Calendar                       updated;

    private Builder(
      final String in_title,
      final String in_id,
      final Calendar in_updated)
    {
      this.title = NullCheck.notNull(in_title);
      this.id = NullCheck.notNull(in_id);
      this.updated = NullCheck.notNull(in_updated);
      this.entries = new ArrayList<OPDSAcquisitionFeedEntry>();
    }

    @Override public void addEntry(
      final OPDSAcquisitionFeedEntry e)
    {
      this.entries.add(NullCheck.notNull(e));
    }

    @Override public OPDSAcquisitionFeed build()
    {
      return new OPDSAcquisitionFeed(
        this.entries,
        this.id,
        this.updated,
        this.title);
    }
  }

  public static OPDSAcquisitionFeedBuilderType newBuilder(
    final String in_id,
    final Calendar in_updated,
    final String in_title)
  {
    return new Builder(in_title, in_id, in_updated);
  }

  private final List<OPDSAcquisitionFeedEntry> entries;
  private final String                         id;
  private final String                         title;
  private final Calendar                       updated;

  private OPDSAcquisitionFeed(
    final List<OPDSAcquisitionFeedEntry> in_entries,
    final String in_id,
    final Calendar in_updated,
    final String in_title)
  {
    this.entries =
      NullCheck.notNull(Collections.unmodifiableList(in_entries));
    this.id = NullCheck.notNull(in_id);
    this.updated = NullCheck.notNull(in_updated);
    this.title = NullCheck.notNull(in_title);
  }

  public List<OPDSAcquisitionFeedEntry> getFeedEntries()
  {
    return this.entries;
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
