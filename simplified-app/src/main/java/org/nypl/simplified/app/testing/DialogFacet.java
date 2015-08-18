package org.nypl.simplified.app.testing;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.catalog.CatalogFacetDialog;
import org.nypl.simplified.books.core.FeedFacetPseudo;
import org.nypl.simplified.books.core.FeedFacetPseudo.FacetType;
import org.nypl.simplified.books.core.FeedFacetType;

import java.util.ArrayList;

/**
 * A facet selection activity.
 */

public final class DialogFacet extends Activity
{
  /**
   * Construct an activity.
   */

  public DialogFacet()
  {

  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final FeedFacetPseudo fa =
      new FeedFacetPseudo("Author", true, FacetType.SORT_BY_AUTHOR);
    final FeedFacetPseudo ft =
      new FeedFacetPseudo("Title", true, FacetType.SORT_BY_TITLE);
    final ArrayList<FeedFacetType> items = new ArrayList<FeedFacetType>();
    for (int index = 0; index < 10; ++index) {
      items.add(fa);
      items.add(ft);
    }

    final CatalogFacetDialog d = CatalogFacetDialog.newDialog("Sort by", items);
    final FragmentManager fm = this.getFragmentManager();
    d.show(fm, "dialog");
  }
}
