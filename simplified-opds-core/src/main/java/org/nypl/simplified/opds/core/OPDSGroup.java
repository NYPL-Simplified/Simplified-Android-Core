package org.nypl.simplified.opds.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.net.URI;
import java.util.List;

/**
 * An OPDS <i>group</i>.
 */

public final class OPDSGroup
{
  private final String                         title;
  private final URI                            uri;
  private final List<OPDSAcquisitionFeedEntry> entries;

  /**
   * Construct an OPDS group.
   *
   * @param in_title   The group title
   * @param in_uri     The group URI
   * @param in_entries The group entries
   */

  public OPDSGroup(
    final String in_title,
    final URI in_uri,
    final List<OPDSAcquisitionFeedEntry> in_entries)
  {
    this.title = NullCheck.notNull(in_title);
    this.uri = NullCheck.notNull(in_uri);
    this.entries = NullCheck.notNull(in_entries);
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.entries.hashCode();
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.uri.hashCode();
    return result;
  }

  @Override public boolean equals(
    final @Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final OPDSGroup other = (OPDSGroup) obj;
    return this.entries.equals(other.entries)
           && this.title.equals(other.title)
           && this.uri.equals(other.uri);
  }

  /**
   * @return The group title
   */

  public String getGroupTitle()
  {
    return this.title;
  }

  /**
   * @return The group URI
   */

  public URI getGroupURI()
  {
    return this.uri;
  }

  /**
   * @return The entries within the group
   */

  public List<OPDSAcquisitionFeedEntry> getGroupEntries()
  {
    return this.entries;
  }
}
