package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;

import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * <p> The type of book availability. </p> <p> OPDS does not have a standard way
 * to signal the availability of a given item, so this implementation determines
 * availability based on extra non-OPDS information added to the feed. </p>
 */

public interface OPDSAvailabilityType extends Serializable {

  /**
   * The date when the availability expires, if there is one
   *
   * @return end_date
   */

  OptionType<DateTime> getEndDate();

  /**
   * Match the type of availability.
   *
   * @param m   The matcher
   * @param <A> The type of returned values
   * @param <E> The type of raised exceptions
   * @return The value returned by the matcher
   * @throws E If the matcher raises {@code E}
   */

  <A, E extends Exception> A matchAvailability(
    OPDSAvailabilityMatcherType<A, E> m)
    throws E;
}
