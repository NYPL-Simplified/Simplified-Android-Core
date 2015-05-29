package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.R;
import org.nypl.simplified.books.core.FeedFacetPseudo.FacetType;
import org.nypl.simplified.books.core.FeedFacetPseudoTitleProviderType;

import android.content.res.Resources;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * A title provider for pseudo facets.
 */

public final class CatalogFacetPseudoTitleProvider implements
  FeedFacetPseudoTitleProviderType
{
  private final Resources rr;

  public CatalogFacetPseudoTitleProvider(
    final Resources in_rr)
  {
    this.rr = NullCheck.notNull(in_rr);
  }

  @Override public String getTitle(
    final FacetType t)
  {
    switch (t) {
      case SORT_BY_AUTHOR:
      {
        return NullCheck.notNull(this.rr
          .getString(R.string.books_sort_by_author));
      }
      case SORT_BY_TITLE:
      {
        return NullCheck.notNull(this.rr
          .getString(R.string.books_sort_by_title));
      }
    }

    throw new UnreachableCodeException();
  }
}
