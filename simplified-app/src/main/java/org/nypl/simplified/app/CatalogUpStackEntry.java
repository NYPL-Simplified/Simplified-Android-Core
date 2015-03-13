package org.nypl.simplified.app;

import java.io.Serializable;
import java.net.URI;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogUpStackEntry implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final String      title;
  private final URI         uri;

  public CatalogUpStackEntry(
    final URI in_uri,
    final String in_title)
  {
    this.uri = NullCheck.notNull(in_uri);
    this.title = NullCheck.notNull(in_title);
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
    final CatalogUpStackEntry other = (CatalogUpStackEntry) obj;
    return this.title.equals(other.title) && this.uri.equals(other.uri);
  }

  public String getTitle()
  {
    return this.title;
  }

  public URI getURI()
  {
    return this.uri;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.uri.hashCode();
    return result;
  }
}
