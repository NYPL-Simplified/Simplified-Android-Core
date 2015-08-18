package org.nypl.simplified.opds.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.io.Serializable;
import java.net.URI;

/**
 * An OPDS <i>facet</i>.
 *
 * See http://opds-spec.org/specs/opds-catalog-1-1-20110627/#Facets.
 */

public final class OPDSFacet implements Serializable
{
  private static final long serialVersionUID = 1L;

  private final boolean active;
  private final URI     uri;
  private final String  group;
  private final String  title;

  /**
   * Construct an OPDS facet.
   *
   * @param in_active {@code true} if the facet is active
   * @param in_uri    The URI
   * @param in_group  The group
   * @param in_title  The title
   */

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

  /**
   * @return {@code true} if the facet is active
   */

  public boolean isActive()
  {
    return this.active;
  }

  /**
   * @return The URI
   */

  public URI getURI()
  {
    return this.uri;
  }

  /**
   * @return The group
   */

  public String getGroup()
  {
    return this.group;
  }

  /**
   * @return The title
   */

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
