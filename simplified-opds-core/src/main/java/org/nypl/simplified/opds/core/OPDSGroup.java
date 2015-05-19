package org.nypl.simplified.opds.core;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class OPDSGroup implements Serializable
{
  private static final long                    serialVersionUID = 1L;
  private final String                         title;
  private final URI                            uri;
  private final List<OPDSAcquisitionFeedEntry> entries;

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

  public OPDSGroup(
    final String in_title,
    final URI in_uri,
    final List<OPDSAcquisitionFeedEntry> in_entries)
  {
    this.title = NullCheck.notNull(in_title);
    this.uri = NullCheck.notNull(in_uri);
    this.entries = NullCheck.notNull(in_entries);
  }

  public String getGroupTitle()
  {
    return this.title;
  }

  public URI getGroupURI()
  {
    return this.uri;
  }

  public List<OPDSAcquisitionFeedEntry> getGroupEntries()
  {
    return this.entries;
  }
}
