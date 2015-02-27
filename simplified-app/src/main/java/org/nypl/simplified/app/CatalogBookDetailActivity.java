package org.nypl.simplified.app;

import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.google.common.collect.ImmutableList;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogBookDetailActivity extends CatalogActivity
{
  private static final String CATALOG_BOOK_DETAIL_FEED_ENTRY_ID;

  static {
    CATALOG_BOOK_DETAIL_FEED_ENTRY_ID =
      "org.nypl.simplified.app.CatalogBookDetailActivity.feed_entry";
  }

  public static void setActivityArguments(
    final Bundle b,
    final boolean drawer_open,
    final ImmutableList<URI> up_stack,
    final OPDSAcquisitionFeedEntry e)
  {
    NullCheck.notNull(b);
    SimplifiedActivity.setActivityArguments(b, drawer_open);
    CatalogActivity.setActivityArguments(b, up_stack);
    b.putSerializable(
      CatalogBookDetailActivity.CATALOG_BOOK_DETAIL_FEED_ENTRY_ID,
      NullCheck.notNull(e));
  }

  public static void startNewActivity(
    final Activity from,
    final ImmutableList<URI> up_stack,
    final OPDSAcquisitionFeedEntry e)
  {
    final Bundle b = new Bundle();
    CatalogBookDetailActivity.setActivityArguments(b, false, up_stack, e);
    final Intent i = new Intent(from, CatalogBookDetailActivity.class);
    i.putExtras(b);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    from.startActivity(i);
  }

  private @Nullable OPDSAcquisitionFeedEntry entry;

  private OPDSAcquisitionFeedEntry getFeedEntry()
  {
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = NullCheck.notNull(i.getExtras());
    return NullCheck
      .notNull((OPDSAcquisitionFeedEntry) a
        .getSerializable(CatalogBookDetailActivity.CATALOG_BOOK_DETAIL_FEED_ENTRY_ID));
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.entry = this.getFeedEntry();
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
  }

  @Override protected void onResume()
  {
    super.onResume();

    final FrameLayout content_area = this.getContentFrame();

    final LayoutInflater inflater =
      NullCheck.notNull(this.getLayoutInflater());
    final View layout =
      inflater.inflate(R.layout.book_detail, content_area, false);

    content_area.removeAllViews();
    content_area.addView(layout);
    content_area.requestLayout();
  }
}
