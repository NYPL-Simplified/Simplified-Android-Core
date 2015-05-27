package org.nypl.simplified.opds.core;

import java.io.Serializable;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * An OPDS/Atom <i>category</i>.
 *
 * @see <a href="http://tools.ietf.org/html/rfc4287#section-4.2.2">RFC4287
 *      categories</a>
 */

public final class OPDSCategory implements Serializable
{
  private static final long serialVersionUID = 2L;

  private final String      scheme;
  private final String      term;

  public OPDSCategory(
    final String in_term,
    final String in_scheme)
  {
    this.term = NullCheck.notNull(in_term);
    this.scheme = NullCheck.notNull(in_scheme);
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
    final OPDSCategory other = (OPDSCategory) obj;
    return this.scheme.equals(other.scheme) && this.term.equals(other.term);
  }

  public String getScheme()
  {
    return this.scheme;
  }

  public String getTerm()
  {
    return this.term;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.scheme.hashCode();
    result = (prime * result) + this.term.hashCode();
    return result;
  }
}
