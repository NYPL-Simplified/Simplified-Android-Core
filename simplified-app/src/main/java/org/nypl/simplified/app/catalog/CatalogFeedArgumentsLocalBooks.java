package org.nypl.simplified.app.catalog;

import org.nypl.simplified.books.core.FeedFacetPseudo;
import org.nypl.simplified.books.core.FeedFacetPseudo.Type;

import com.io7m.jnull.NullCheck;

public final class CatalogFeedArgumentsLocalBooks implements
  CatalogFeedArgumentsType
{
  private static final long serialVersionUID = 1L;

  private final Type        facet_type;
  private final String      title;

  public CatalogFeedArgumentsLocalBooks(
    final String in_title,
    final FeedFacetPseudo.Type in_facet_type)
  {
    this.title = NullCheck.notNull(in_title);
    this.facet_type = NullCheck.notNull(in_facet_type);
  }

  public Type getFacetType()
  {
    return this.facet_type;
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
