package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import java.math.BigInteger;
import java.net.URI;

/**
 * A generic OPDS link. These are typically used in authentication documents.
 *
 * @see OPDSAuthenticationDocument
 */

public final class OPDSLink
{
  private final URI                    href;
  private final OptionType<String>     type;
  private final OptionType<BigInteger> length;
  private final OptionType<String>     hash;

  /**
   * Construct an OPDS link.
   *
   * @param in_hash      The optional hash of the target content
   * @param in_href      The URI of the target content
   * @param in_type      The optional type of the target content
   * @param in_length    The optional length of the target content
   */

  public OPDSLink(
    final OptionType<String> in_hash,
    final URI in_href,
    final OptionType<String> in_type,
    final OptionType<BigInteger> in_length)
  {
    this.hash = NullCheck.notNull(in_hash);
    this.href = NullCheck.notNull(in_href);
    this.type = NullCheck.notNull(in_type);
    this.length = NullCheck.notNull(in_length);
  }

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("OPDSLink{");
    sb.append("hash=").append(this.hash);
    sb.append(", href=").append(this.href);
    sb.append(", type=").append(this.type);
    sb.append(", length=").append(this.length);
    sb.append('}');
    return sb.toString();
  }

  /**
   * @return The optional hash of the target content
   */

  public OptionType<String> getHash()
  {
    return this.hash;
  }

  /**
   * @return The URI of the target content
   */

  public URI getHref()
  {
    return this.href;
  }

  /**
   * @return The optional length of the target content
   */

  public OptionType<BigInteger> getLength()
  {
    return this.length;
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final OPDSLink other = (OPDSLink) o;
    if (!this.getHref().equals(other.getHref())) {
      return false;
    }
    if (!this.getType().equals(other.getType())) {
      return false;
    }
    if (!this.getLength().equals(other.getLength())) {
      return false;
    }
    return this.getHash().equals(other.getHash());
  }

  @Override public int hashCode()
  {
    int result = this.getHref().hashCode();
    result = 31 * result + this.getType().hashCode();
    result = 31 * result + this.getLength().hashCode();
    result = 31 * result + this.getHash().hashCode();
    return result;
  }

  /**
   * @return The optional type of the target content
   */

  public OptionType<String> getType()
  {
    return this.type;
  }
}
