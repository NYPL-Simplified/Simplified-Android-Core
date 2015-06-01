package org.nypl.simplified.app.catalog;

import java.io.Serializable;

import org.nypl.simplified.stack.ImmutableStack;

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
   * @param m
   *          The matcher
   * @return The value returned by the matcher
   * @throws E
   *           Propagated from the matcher
   */

  <A, E extends Exception> A matchArguments(
    CatalogFeedArgumentsMatcherType<A, E> m)
    throws E;
}
