package org.nypl.simplified.opds.core;

import java.io.Serializable;
import java.net.URI;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * A specific OPDS acquisition.
 * 
 * @see <a
 *      href="http://opds-spec.org/specs/opds-catalog-1-1-20110627/#Acquisition_Feeds">Acquisiton
 *      feeds</a>
 */

public final class OPDSAcquisition implements Serializable
{
  /**
   * The specific type of acquisition.
   */

  public static enum Type
  {
    ACQUISITION_BORROW(NullCheck.notNull(URI
      .create("http://opds-spec.org/acquisition/borrow"))),
    ACQUISITION_BUY(NullCheck.notNull(URI
      .create("http://opds-spec.org/acquisition/buy"))),
    ACQUISITION_GENERIC(NullCheck.notNull(URI
      .create("http://opds-spec.org/acquisition"))),
    ACQUISITION_OPEN_ACCESS(NullCheck.notNull(URI
      .create("http://opds-spec.org/acquisition/open-access"))),
    ACQUISITION_SAMPLE(NullCheck.notNull(URI
      .create("http://opds-spec.org/acquisition/sample"))),
    ACQUISITION_SUBSCRIBE(NullCheck.notNull(URI
      .create("http://opds-spec.org/acquisition/subscribe")));

    private final URI uri;

    private Type(
      final URI in_uri)
    {
      this.uri = NullCheck.notNull(in_uri);
    }

    public URI getURI()
    {
      return this.uri;
    }
  }

  private static final long serialVersionUID = 8499594912565063700L;

  private final Type        type;
  private final URI         uri;

  public OPDSAcquisition(
    final Type in_type,
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
    final OPDSAcquisition other = (OPDSAcquisition) obj;
    return (this.type == other.type) && this.uri.equals(other.uri);
  }

  public Type getType()
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

  @Override public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("[OPDSAcquisition ");
    builder.append(this.type);
    builder.append(" → ");
    builder.append(this.uri);
    builder.append("]");
    return NullCheck.notNull(builder.toString());
  }
}
