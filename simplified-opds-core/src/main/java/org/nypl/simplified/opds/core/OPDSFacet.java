package org.nypl.simplified.opds.core;

import java.net.URI;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * An OPDS <i>facet</i>.
 *
 * @see <a
 *      href="http://opds-spec.org/specs/opds-catalog-1-1-20110627/#Facets">Facets</a>
 */

public final class OPDSFacet
{
  private final boolean active;
  private final URI     uri;
  private final String  group;
  private final String  title;

  public OPDSFacet(
    final boolean in_active,
    final URI in_uri,
    final String in_group,
    final String in_title)
  {
    this.active = in_active;
    this.uri = NullCheck.notNull(in_uri);
    this.group = NullCheck.notNull(in_group);
    this.title = NullCheck.notNull(in_title);
  }

  public boolean isActive()
  {
    return this.active;
  }

  public URI getURI()
  {
    return this.uri;
  }

  public String getGroup()
  {
    return this.group;
  }

  public String getTitle()
  {
    return this.title;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + (this.active ? 1231 : 1237);
    result = (prime * result) + this.group.hashCode();
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
    final OPDSFacet other = (OPDSFacet) obj;
    return (this.active == other.active)
      && this.group.equals(other.group)
      && this.title.equals(other.title)
      && this.uri.equals(other.uri);
  }
}
