package org.nypl.simplified.app.catalog;

public interface CatalogFeedArgumentsMatcherType<A, E extends Exception>
{
  A onFeedArgumentsLocalBooks(
    CatalogFeedArgumentsLocalBooks c)
    throws E;

  A onFeedArgumentsRemote(
    CatalogFeedArgumentsRemote c)
    throws E;
}
