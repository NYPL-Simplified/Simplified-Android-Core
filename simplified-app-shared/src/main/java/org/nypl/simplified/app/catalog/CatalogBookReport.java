package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.view.View;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

/**
 * A controller that opens a report activity for a given book.
 */
public final class CatalogBookReport implements View.OnClickListener {

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogBookRead.class);
  }

  private final Activity activity;
  private final FeedEntryOPDS feed_entry;

  /**
   * The parent activity.
   *
   * @param in_activity       The activity
   * @param in_feed_entry     Book feed entry
   */

  public CatalogBookReport(
    final Activity in_activity,
    final FeedEntryOPDS in_feed_entry)
  {
    this.activity = NullCheck.notNull(in_activity);
    this.feed_entry = NullCheck.notNull(in_feed_entry);
  }

  @Override
  public void onClick(
    final @Nullable View v) {
    CatalogBookReportActivity.startActivity(this.activity, this.feed_entry);
  }
}
