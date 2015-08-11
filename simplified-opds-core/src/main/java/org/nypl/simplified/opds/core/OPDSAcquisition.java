package org.nypl.simplified.opds.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.io.Serializable;
import java.net.URI;

/**
 * A specific OPDS acquisition.
 *
 * http://opds-spec.org/specs/opds-catalog-1-1-20110627/#Acquisition_Feeds
 */

public final class OPDSAcquisition implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final Type type;
  private final URI  uri;

  /**
   * Construct an acquisition.
   *
   * @param in_type The type
   * @param in_uri  The URI
   */

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

  /**
   * @return The type of the acquisition
   */

  public Type getType()
  {
    return this.type;
  }

  /**
   * @return The URI of the acquisition
   */

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
    final StringBuilder builder = new StringBuilder(64);
    builder.append("[OPDSAcquisition ");
    builder.append(this.type);
    builder.append(" → ");
    builder.append(this.uri);
    builder.append("]");
    return NullCheck.notNull(builder.toString());
  }

  /**
   * The specific type of acquisition.
   */

  public enum Type
  {
    /**
     * An item can be borrowed.
     */

    ACQUISITION_BORROW(
      NullCheck.notNull(
        URI.create("http://opds-spec.org/acquisition/borrow"))),

    /**
     * An item can be bought.
     */

    ACQUISITION_BUY(
      NullCheck.notNull(
        URI.create("http://opds-spec.org/acquisition/buy"))),

    /**
     * An item can be obtained.
     */

    ACQUISITION_GENERIC(
      NullCheck.notNull(
        URI.create("http://opds-spec.org/acquisition"))),

    /**
     * An item is open access (possibly public domain).
     */

    ACQUISITION_OPEN_ACCESS(
      NullCheck.notNull(
        URI.create("http://opds-spec.org/acquisition/open-access"))),

    /**
     * An item can be sampled.
     */

    ACQUISITION_SAMPLE(
      NullCheck.notNull(
        URI.create("http://opds-spec.org/acquisition/sample"))),

    /**
     * An item can be subscribed to.
     */

    ACQUISITION_SUBSCRIBE(
      NullCheck.notNull(
        URI.create("http://opds-spec.org/acquisition/subscribe")));

    private final URI uri;

    Type(
      final URI in_uri)
    {
      this.uri = NullCheck.notNull(in_uri);
    }

    /**
     * @return The relation URI
     */

    public URI getURI()
    {
      return this.uri;
    }
  }
}
