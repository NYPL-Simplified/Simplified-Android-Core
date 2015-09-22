package org.nypl.simplified.app.catalog;

/**
 * A matcher for different types of feed arguments.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface CatalogFeedArgumentsMatcherType<A, E extends Exception>
{
  /**
   * The matched value is a {@link CatalogFeedArgumentsLocalBooks}.
   *
   * @param c The value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFeedArgumentsLocalBooks(
    CatalogFeedArgumentsLocalBooks c)
    throws E;

  /**
   * The matched value is a {@link CatalogFeedArgumentsRemote}.
   *
   * @param c The value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFeedArgumentsRemote(
    CatalogFeedArgumentsRemote c)
    throws E;
}
