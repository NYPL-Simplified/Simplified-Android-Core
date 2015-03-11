package org.nypl.simplified.opds.core;

import java.net.URI;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The type of search links.
 */

public final class OPDSSearchLink
{
  private final String type;
  private final URI    uri;

  public OPDSSearchLink(
    final String in_type,
    final URI in_uri)
  {
    this.type = NullCheck.notNull(in_type);
    this.uri = NullCheck.notNull(in_uri);
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
    final OPDSSearchLink other = (OPDSSearchLink) obj;
    return this.type.equals(other.type) && this.uri.equals(other.uri);
  }

  public String getType()
  {
    return this.type;
  }

  public URI getURI()
  {
    return this.uri;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.type.hashCode();
    result = (prime * result) + this.uri.hashCode();
    return result;
  }
}
