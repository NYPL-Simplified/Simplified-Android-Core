package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.SimplifiedPart;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.books.core.BooksFeedSelection;
import org.slf4j.Logger;

/**
 * The activity that displays the currently loaned and/or downloaded books.
 */

public final class MainBooksActivity extends MainLocalFeedActivity
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainBooksActivity.class);
  }

  /**
   * Construct an activity.
   */

  public MainBooksActivity()
  {

  }

  @Override protected BooksFeedSelection getLocalFeedTypeSelection()
  {
    return BooksFeedSelection.BOOKS_FEED_LOANED;
  }

  @Override protected SimplifiedPart navigationDrawerGetPart()
  {
    return SimplifiedPart.PART_BOOKS;
  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return true;
  }

  @Override protected String catalogFeedGetEmptyText()
  {
    return this.getResources().getString(R.string.catalog_empty_my_books);
  }
}
