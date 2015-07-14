package org.nypl.simplified.opds.core;

import java.io.Serializable;

/**
 * <p>
 * The type of book availability.
 * </p>
 * <p>
 * OPDS does not have a standard way to signal the availability of a given
 * item, so this implementation determines availability based on extra
 * non-OPDS information added to the feed.
 * </p>
 */

public interface OPDSAvailabilityType extends Serializable
{
  /**
   * Match the type of availability.
   *
   * @param m
   *          The matcher
   * @return The value returned by the matcher
   * @throws E
   *           If the matcher raises <tt>E</tt>
   */

  <A, E extends Exception> A matchAvailability(
    OPDSAvailabilityMatcherType<A, E> m)
    throws E;
}
