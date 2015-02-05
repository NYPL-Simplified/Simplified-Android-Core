package org.nypl.simplified.opds.core;

import java.net.URI;

import com.io7m.jnull.NullCheck;

public final class OPDSAcquisition
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

  private final Type type;
  private final URI  uri;

  public OPDSAcquisition(
    final Type in_type,
    final URI in_uri)
  {
    this.type = NullCheck.notNull(in_type);
    this.uri = NullCheck.notNull(in_uri);
  }

  public Type getType()
  {
    return this.type;
  }

  public URI getURI()
  {
    return this.uri;
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
