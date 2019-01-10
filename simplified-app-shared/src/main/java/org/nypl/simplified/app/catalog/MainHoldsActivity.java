package org.nypl.simplified.app.catalog;

import android.content.res.Resources;

import org.nypl.simplified.app.R;
import org.nypl.simplified.books.feeds.FeedBooksSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An activity that displays the holds for the current user.
 */

public final class MainHoldsActivity extends MainLocalFeedActivity
{
  private static final Logger LOG = LoggerFactory.getLogger(MainHoldsActivity.class);

  @Override
  protected Logger log() {
    return LOG;
  }

  /**
   * Construct a new activity.
   */

  public MainHoldsActivity()
  {

  }

  @Override protected FeedBooksSelection localFeedTypeSelection()
  {
    return FeedBooksSelection.BOOKS_FEED_HOLDS;
  }

  @Override
  protected String navigationDrawerGetActivityTitle(final Resources resources) {
    return resources.getString(R.string.holds);
  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return true;
  }

  @Override protected String catalogFeedGetEmptyText()
  {
    return this.getResources().getString(R.string.holds_empty);
  }
}
