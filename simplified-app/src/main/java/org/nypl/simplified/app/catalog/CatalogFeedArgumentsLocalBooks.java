package org.nypl.simplified.app.catalog;

import org.nypl.simplified.books.core.FeedFacetPseudo;
import org.nypl.simplified.books.core.FeedFacetPseudo.FacetType;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

public final class CatalogFeedArgumentsLocalBooks implements
  CatalogFeedArgumentsType
{
  private static final long        serialVersionUID = 1L;

  private final FacetType               facet_type;
  private final OptionType<String> search_terms;
  private final String             title;

  public CatalogFeedArgumentsLocalBooks(
    final String in_title,
    final FeedFacetPseudo.FacetType in_facet_type,
    final OptionType<String> in_search_terms)
  {
    this.title = NullCheck.notNull(in_title);
    this.facet_type = NullCheck.notNull(in_facet_type);
    this.search_terms = NullCheck.notNull(in_search_terms);
  }

  public FacetType getFacetType()
  {
    return this.facet_type;
  }

  public OptionType<String> getSearchTerms()
  {
    return this.search_terms;
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
