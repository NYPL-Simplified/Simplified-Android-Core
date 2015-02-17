package org.nypl.simplified.app;

/**
 * The type of part matchers.
 *
 * @param <A>
 *          The type of returned values
 * @param <E>
 *          The type of raised exceptions
 */

public interface PartActivityMatcherType<A, E extends Exception>
{
  /**
   * Match an activity.
   *
   * @param a
   *          An activity
   * @return A value of <code>A</code>
   * @throws E
   *           If required
   */

  A books(
    final BooksActivity a)
    throws E;

  /**
   * Match an activity.
   *
   * @param a
   *          An activity
   * @return A value of <code>A</code>
   * @throws E
   *           If required
   */

  A catalog(
    final CatalogActivity a)
    throws E;

  /**
   * Match an activity.
   *
   * @param a
   *          An activity
   * @return A value of <code>A</code>
   * @throws E
   *           If required
   */

  A holds(
    final HoldsActivity a)
    throws E;

  /**
   * Match an activity.
   *
   * @param a
   *          An activity
   * @return A value of <code>A</code>
   * @throws E
   *           If required
   */

  A settings(
    final SettingsActivity a)
    throws E;
}
