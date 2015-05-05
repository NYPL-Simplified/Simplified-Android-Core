package org.nypl.simplified.app.catalog;

import com.io7m.jnull.NullCheck;

public final class CatalogFeedArgumentsLocalBooks implements
  CatalogFeedArgumentsType
{
  private static final long serialVersionUID = 1L;

  private final String      title;

  public CatalogFeedArgumentsLocalBooks(
    final String in_title)
  {
    this.title = NullCheck.notNull(in_title);
  }

  @Override public String getTitle()
  {
    return this.title;
  }

  @Override public <A, E extends Exception> A matchArguments(
    final CatalogFeedArgumentsMatcherType<A, E> m)
    throws E
  {
    return m.onFeedArgumentsLocalBooks(this);
  }
}
