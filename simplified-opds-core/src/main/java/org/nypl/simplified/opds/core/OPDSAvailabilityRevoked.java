package org.nypl.simplified.opds.core;

import com.io7m.jnull.NullCheck;

import java.net.URI;

/**
 * The has been revoked via whatever DRM system it uses, but the server has yet
 * to be notified of this fact.
 */

public final class OPDSAvailabilityRevoked implements OPDSAvailabilityType
{
  private static final long serialVersionUID = 1L;
  private final URI revoke;

  private OPDSAvailabilityRevoked(final URI in_revoke)
  {
    this.revoke = NullCheck.notNull(in_revoke);
  }

  /**
   * @param revoke The revocation link
   *
   * @return A "revoked" availability value
   */

  public static OPDSAvailabilityRevoked get(
    final URI revoke)
  {
    return new OPDSAvailabilityRevoked(revoke);
  }

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("OPDSAvailabilityRevoked{");
    sb.append("revoke=").append(this.revoke);
    sb.append('}');
    return sb.toString();
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final OPDSAvailabilityRevoked that = (OPDSAvailabilityRevoked) o;
    return this.getRevoke().equals(that.getRevoke());
  }

  @Override public int hashCode()
  {
    return this.getRevoke().hashCode();
  }

  @Override public <A, E extends Exception> A matchAvailability(
    final OPDSAvailabilityMatcherType<A, E> m)
    throws E
  {
    return m.onRevoked(this);
  }

  /**
   * @return The revocation link
   */

  public URI getRevoke()
  {
    return this.revoke;
  }
}
