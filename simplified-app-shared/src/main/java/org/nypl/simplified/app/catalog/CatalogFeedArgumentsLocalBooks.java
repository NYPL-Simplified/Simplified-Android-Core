package org.nypl.simplified.app.catalog;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.feeds.FeedBooksSelection;
import org.nypl.simplified.books.feeds.FeedFacetPseudo;
import org.nypl.simplified.books.feeds.FeedFacetPseudo.FacetType;
import org.nypl.simplified.stack.ImmutableStack;

/**
 * Arguments given to a {@link CatalogFeedActivity} that indicate that the feed
 * to be displayed is the local books feed.
 */

public final class CatalogFeedArgumentsLocalBooks implements CatalogFeedArgumentsType {
  private static final long serialVersionUID = 1L;

  private final FacetType facet_type;
  private final OptionType<String> search_terms;
  private final String title;
  private final ImmutableStack<CatalogFeedArgumentsType> up_stack;
  private final FeedBooksSelection selection;

  /**
   * Construct feed arguments.
   *
   * @param in_up_stack     The new up-stack
   * @param in_title        The feed title
   * @param in_facet_type   The current facet type
   * @param in_search_terms The search terms, if any
   * @param in_selection    The type of local books feed to generate
   */

  public CatalogFeedArgumentsLocalBooks(
    final ImmutableStack<CatalogFeedArgumentsType> in_up_stack,
    final String in_title,
    final FeedFacetPseudo.FacetType in_facet_type,
    final OptionType<String> in_search_terms,
    final FeedBooksSelection in_selection) {
    this.up_stack = NullCheck.notNull(in_up_stack);
    this.title = NullCheck.notNull(in_title);
    this.facet_type = NullCheck.notNull(in_facet_type);
    this.search_terms = NullCheck.notNull(in_search_terms);
    this.selection = NullCheck.notNull(in_selection);
  }

  /**
   * @return The current facet type
   */

  public FacetType getFacetType() {
    return this.facet_type;
  }

  /**
   * @return The type of books feed that will be generated
   */

  public FeedBooksSelection getSelection() {
    return this.selection;
  }

  /**
   * @return The search terms, if any
   */

  public OptionType<String> getSearchTerms() {
    return this.search_terms;
  }

  @Override
  public String getTitle() {
    return this.title;
  }

  @Override
  public ImmutableStack<CatalogFeedArgumentsType> getUpStack() {
    return this.up_stack;
  }

  @Override
  public <A, E extends Exception> A matchArguments(
    final CatalogFeedArgumentsMatcherType<A, E> m)
    throws E {
    return m.onFeedArgumentsLocalBooks(this);
  }

  @Override
  public boolean requiresNetworkConnectivity() {
    return false;
  }

  @Override
  public boolean isSearching() {
    return this.search_terms.isSome();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CatalogFeedArgumentsLocalBooks{");
    sb.append("facet_type=").append(this.facet_type);
    sb.append(", search_terms=").append(this.search_terms);
    sb.append(", title='").append(this.title).append('\'');
    sb.append(", up_stack=").append(this.up_stack);
    sb.append(", selection=").append(this.selection);
    sb.append('}');
    return sb.toString();
  }
}
