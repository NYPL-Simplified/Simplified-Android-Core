package org.nypl.simplified.app.catalog;

import org.nypl.simplified.stack.ImmutableStack;

import java.io.Serializable;

/**
 * The type of catalog feed arguments.
 */

public interface CatalogFeedArgumentsType extends Serializable
{
  /**
   * @return The title of the feed
   */

  String getTitle();

  /**
   * @return The up stack
   */

  ImmutableStack<CatalogFeedArgumentsType> getUpStack();

  /**
   * Match on the type of feed.
   *
   * @param m   The matcher
   * @param <A> The type of values returned by the matcher
   * @param <E> The type of exceptions raised by the matcher
   *
   * @return The value returned by the matcher
   *
   * @throws E Propagated from the matcher
   */

  <A, E extends Exception> A matchArguments(
    CatalogFeedArgumentsMatcherType<A, E> m)
    throws E;

  /**
   * @return {@code true} If the feed requires network connectivity to read
   */

  boolean requiresNetworkConnectivity();

  /**
   * @return {@code true} If the feed is the result of a search query.
   */

  boolean isSearching();
}
