package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.slf4j.Logger;

import android.view.View;
import android.view.View.OnClickListener;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogBookDelete implements OnClickListener
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogBookDelete.class);
  }

  private final BookID        id;

  public CatalogBookDelete(
    final BookID in_id)
  {
    this.id = NullCheck.notNull(in_id);
  }

  @Override public void onClick(
    final @Nullable View v)
  {
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    books.bookDeleteData(this.id);
  }
}
