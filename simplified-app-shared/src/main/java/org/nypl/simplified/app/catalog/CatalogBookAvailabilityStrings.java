package org.nypl.simplified.app.catalog;

import android.content.res.Resources;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.nypl.simplified.app.R;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;

import java.util.concurrent.TimeUnit;

/**
 * Functions that map book availability values to human-readable strings.
 */

public final class CatalogBookAvailabilityStrings
{
  private CatalogBookAvailabilityStrings()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Produce a human-readable string for the given ODPS availability value.
   *
   * @param r The application resources
   * @param s The availability value
   *
   * @return A descriptive string
   */

  public static String getOPDSAvailabilityString(
    final Resources r,
    final OPDSAvailabilityType s)
  {
    NullCheck.notNull(r);
    NullCheck.notNull(s);

    return s.matchAvailability(
      new OPDSAvailabilityMatcherType<String, UnreachableCodeException>()
      {
        @Override
        public String onHeldReady(final OPDSAvailabilityHeldReady a)
        {
          return CatalogBookAvailabilityStrings.onReserved(a.getEndDate(), r);
        }

        @Override
        public String onHeld(final OPDSAvailabilityHeld a)
        {
          return CatalogBookAvailabilityStrings.onHeld(
            a.getEndDate(), a.getPosition(), r);
        }

        @Override
        public String onHoldable(final OPDSAvailabilityHoldable a)
        {
          return CatalogBookAvailabilityStrings.onHoldable(r);
        }

        @Override
        public String onLoaned(final OPDSAvailabilityLoaned a)
        {
          return CatalogBookAvailabilityStrings.onLoaned(a.getEndDate(), r);
        }

        @Override
        public String onLoanable(final OPDSAvailabilityLoanable a)
        {
          return CatalogBookAvailabilityStrings.onLoanable(r);
        }

        @Override
        public String onOpenAccess(final OPDSAvailabilityOpenAccess a)
        {
          return CatalogBookAvailabilityStrings.onOpenAccess(r);
        }

        @Override
        public String onRevoked(final OPDSAvailabilityRevoked a)
        {
          return CatalogBookAvailabilityStrings.onRevoked(r);
        }
      });
  }

  private static String onRevoked(final Resources r)
  {
    return r.getString(R.string.catalog_book_availability_revoked);
  }

  private static String onOpenAccess(final Resources r)
  {
    return r.getString(R.string.catalog_book_availability_open_access);
  }

  private static String onLoanable(final Resources r)
  {
    return r.getString(R.string.catalog_book_availability_loanable);
  }

  private static String onHoldable(final Resources r)
  {
    return r.getString(R.string.catalog_book_availability_holdable);
  }

  private static String onLoaned(
    final OptionType<DateTime> expiry_opt,
    final Resources r)
  {
    /**
     * If there is an expiry time, display it.
     */

    if (expiry_opt.isSome()) {
      final Some<DateTime> expiry_some = (Some<DateTime>) expiry_opt;
      final DateTime expiry = expiry_some.get();
      final DateTime now = DateTime.now();
      final String format =
        r.getString(R.string.catalog_book_availability_loaned_timed);

      return String.format(
        format,
        CatalogBookAvailabilityStrings.getIntervalString(r, now, expiry));
    }

    /**
     * Otherwise, show an indefinite loan.
     */

    return r.getString(
      R.string.catalog_book_availability_loaned_indefinite);
  }

  private static String onReserved(
    final OptionType<DateTime> expiry_opt,
    final Resources r)
  {
    /**
     * If there is an expiry time, display it.
     */

    if (expiry_opt.isSome()) {
      final Some<DateTime> expiry_some = (Some<DateTime>) expiry_opt;
      final DateTime expiry = expiry_some.get();
      final DateTime now = DateTime.now();
      final String format =
        r.getString(R.string.catalog_book_availability_reserved_timed);

      return String.format(
        format,
        CatalogBookAvailabilityStrings.getIntervalString(r, now, expiry));
    }

    /**
     * Otherwise, show an indefinite reservation.
     */

    return r.getString(
      R.string.catalog_book_availability_reserved_indefinite);
  }

  private static String onHeld(
    final OptionType<DateTime> end_date_opt,
    final OptionType<Integer> queue_opt,
    final Resources r)
  {
    /**
     * If there is an availability date, show this in preference to
     * anything else.
     */

    if (end_date_opt.isSome()) {
      final Some<DateTime> end_date_some = (Some<DateTime>) end_date_opt;
      final DateTime end_date = end_date_some.get();
      final DateTime now = DateTime.now();
      final String format =
        r.getString(R.string.catalog_book_availability_held_timed);
      return String.format(
        format, CatalogBookAvailabilityStrings.getIntervalString(
          r, now, end_date));
    }

    /**
     * If there is a queue position, attempt to show this instead.
     */

    if (queue_opt.isSome()) {
      final Some<Integer> queue_some = (Some<Integer>) queue_opt;
      final Integer queue = queue_some.get();
      final String format =
        r.getString(R.string.catalog_book_availability_held_queue);
      return String.format(format, queue);
    }

    /**
     * Otherwise, show an indefinite hold.
     */

    return r.getString(
      R.string.catalog_book_availability_held_indefinite);
  }

  //@formatter:off

  /**
   * <p>Construct a time interval string based on the given times. The string
   * will be a localized form of:</p>
   *
   * <ul>
   *   <li>{@code less than an hour} iff the period is under one hour</li>
   *   <li>{@code n hours} iff the period is under one day</li>
   *   <li>{@code n days} otherwise</li>
   * </ul>
   *
   * @param r The application resources
   * @param lower The lower bound of the time period
   * @param upper The upper bound of the time period
   *
   * @return A time interval string
   */

  //@formatter:on
  public static String getIntervalString(
    final Resources r,
    final DateTime lower,
    final DateTime upper)
  {
    NullCheck.notNull(r);
    NullCheck.notNull(lower);
    NullCheck.notNull(upper);

    final long hours =
      CatalogBookAvailabilityStrings.calendarHoursBetween(lower, upper);

    if (hours < 1) {
      return r.getString(R.string.catalog_book_interval_sub_hour);
    }
    if (hours < 24) {
      final String base = r.getString(R.string.catalog_book_interval_hours);
      return String.format("%d %s", hours, base);
    }

    final String base = r.getString(R.string.catalog_book_interval_days);
    return String.format("%d %s", TimeUnit.HOURS.toDays(hours), base);
  }

  /**
   * Construct a short time interval string like "3w", with units up to a year.
   *
   * @param r The application resources
   * @param lower The lower bound of the time period
   * @param upper The upper bound of the time period
   *
   * @return A time interval string
   */
  public static String getIntervalStringShort(
    final Resources r,
    final DateTime lower,
    final DateTime upper)
  {
    NullCheck.notNull(r);
    NullCheck.notNull(lower);
    NullCheck.notNull(upper);

    final long hours =
      CatalogBookAvailabilityStrings.calendarHoursBetween(lower, upper);
    final long days = TimeUnit.HOURS.toDays(hours);
    final long weeks = days / 7;
    final long months = days / 30;
    final long years = days / 365;

    String unit = "";
    long value = 0;

    if (years > 0) {
      unit = r.getString(R.string.catalog_book_interval_years_short);
      value = years;
    } else if (weeks > 8) {
      unit = r.getString(R.string.catalog_book_interval_months_short);
      value = months;
    } else if (weeks > 0) {
      unit = r.getString(R.string.catalog_book_interval_weeks_short);
      value = weeks;
    } else if (days > 0) {
      unit = r.getString(R.string.catalog_book_interval_days_short);
      value = days;
    } else {
      unit = r.getString(R.string.catalog_book_interval_hours_short);
      value = hours;
    }

    return String.format("%d%s", value, unit);
  }

  private static long calendarHoursBetween(
    final DateTime in_start,
    final DateTime in_end)
  {
    return Hours.hoursBetween(in_start, in_end).getHours();
  }
}
