package org.nypl.simplified.opds.core;

import java.io.Serializable;
import java.net.URI;
import java.util.Calendar;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The type of entries in navigation feeds.
 */

public final class OPDSNavigationFeedEntry implements Serializable
{
  private static final long serialVersionUID = 5962627554241694950L;

  public static OPDSNavigationFeedEntry newEntry(
    final String in_id,
    final String in_title,
    final Calendar in_updated,
    final OptionType<URI> in_featured,
    final URI in_target)
  {
    return new OPDSNavigationFeedEntry(
      in_id,
      in_title,
      in_updated,
      in_featured,
      in_target);
  }

  private final OptionType<URI> featured;
  private final String          id;
  private final URI             target;
  private final String          title;
  private final Calendar        updated;

  private OPDSNavigationFeedEntry(
    final String in_id,
    final String in_title,
    final Calendar in_updated,
    final OptionType<URI> in_featured,
    final URI in_target)
  {
    this.id = NullCheck.notNull(in_id);
    this.title = NullCheck.notNull(in_title);
    this.updated = NullCheck.notNull(in_updated);
    this.featured = NullCheck.notNull(in_featured);
    this.target = NullCheck.notNull(in_target);
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
    final OPDSNavigationFeedEntry other = (OPDSNavigationFeedEntry) obj;
    return this.featured.equals(other.featured)
      && this.id.equals(other.id)
      && this.target.equals(other.target)
      && this.title.equals(other.title)
      && this.updated.equals(other.updated);
  }

  public OptionType<URI> getFeaturedURI()
  {
    return this.featured;
  }

  public String getID()
  {
    return this.id;
  }

  public URI getTargetURI()
  {
    return this.target;
  }

  public String getTitle()
  {
    return this.title;
  }

  public Calendar getUpdated()
  {
    return this.updated;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.featured.hashCode();
    result = (prime * result) + this.id.hashCode();
    result = (prime * result) + this.target.hashCode();
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.updated.hashCode();
    return result;
  }
}
