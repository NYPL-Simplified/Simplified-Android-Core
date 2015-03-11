package org.nypl.simplified.app;

import java.net.URI;
import java.util.concurrent.CancellationException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogBookDetailActivity extends CatalogActivity
{
  private static final String CATALOG_BOOK_DETAIL_FEED_ENTRY_ID;
  private static final String TAG;

  static {
    CATALOG_BOOK_DETAIL_FEED_ENTRY_ID =
      "org.nypl.simplified.app.CatalogBookDetailActivity.feed_entry";
    TAG = "CBDA";
  }

  /**
   * Configure the given web view to match the height of the rendered content.
   */

  private static void configureSummaryWebViewHeight(
    final WebView summary_text)
  {
    final LinearLayout.LayoutParams q =
      new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT);
    summary_text.setLayoutParams(q);
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
  private @Nullable ListenableFuture<Bitmap> loading_cover;

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

    final ListenableFuture<Bitmap> lc = this.loading_cover;
    if (lc != null) {
      lc.cancel(true);
    }
  }

  @Override protected void onResume()
  {
    super.onResume();

    final ScrollView sv = new ScrollView(this);
    final LayoutParams p =
      new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    sv.setLayoutParams(p);
    sv.addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override public void onLayoutChange(
        final @Nullable View v,
        final int left,
        final int top,
        final int right,
        final int bottom,
        final int oldLeft,
        final int oldTop,
        final int oldRight,
        final int oldBottom)
      {
        sv.setScrollY(0);
      }
    });

    final LayoutInflater inflater =
      NullCheck.notNull(this.getLayoutInflater());

    final View layout = inflater.inflate(R.layout.book_dialog, sv, false);
    sv.addView(layout);

    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry);
    final Resources rr = NullCheck.notNull(this.getResources());

    final ViewGroup header =
      NullCheck.notNull((ViewGroup) layout.findViewById(R.id.book_header));
    final ViewGroup header_left =
      NullCheck.notNull((ViewGroup) header
        .findViewById(R.id.book_header_left));
    final TextView header_title =
      NullCheck.notNull((TextView) header
        .findViewById(R.id.book_header_title));
    final TextView header_subtitle =
      NullCheck.notNull((TextView) header
        .findViewById(R.id.book_header_subtitle));
    final ImageView header_cover =
      NullCheck.notNull((ImageView) header
        .findViewById(R.id.book_header_cover));
    final TextView header_authors =
      NullCheck.notNull((TextView) header
        .findViewById(R.id.book_header_authors));
    final TextView header_meta =
      NullCheck
        .notNull((TextView) header.findViewById(R.id.book_header_meta));

    final ViewGroup hold_notification =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_hold_notification));

    final ViewGroup acquisitions =
      NullCheck.notNull((ViewGroup) layout.findViewById(R.id.book_buttons));

    final ViewGroup summary =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_summary_layout));
    final TextView summary_publisher =
      NullCheck.notNull((TextView) summary
        .findViewById(R.id.book_summary_publisher));
    final WebView summary_text =
      NullCheck.notNull((WebView) summary
        .findViewById(R.id.book_summary_text));

    final ViewGroup related_layout =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_related_layout));

    /**
     * Assuming a roughly fixed height for cover images, assume a 4:3 aspect
     * ratio and set the width of the cover layout.
     */

    final int cover_height = header_cover.getLayoutParams().height;
    final int cover_width = (int) ((cover_height / 4.0) * 3.0);
    final LinearLayout.LayoutParams cp =
      new LinearLayout.LayoutParams(cover_width, LayoutParams.WRAP_CONTENT);
    header_left.setLayoutParams(cp);

    /**
     * Configure detail texts.
     */

    CatalogBookDetail.configureSummaryPublisher(e, summary_publisher);
    CatalogBookDetail.configureAcquisitions(this, e, acquisitions);
    CatalogBookDetail.configureSummaryWebView(e, summary_text);
    CatalogBookDetailActivity.configureSummaryWebViewHeight(summary_text);

    hold_notification.setVisibility(View.GONE);
    header_title.setText(e.getTitle());

    if (e.getSubtitle().isEmpty() == false) {
      header_subtitle.setText(e.getSubtitle());
    } else {
      header_subtitle.setVisibility(View.GONE);
    }

    CatalogBookDetail.configureViewTextAuthor(e, header_authors);
    CatalogBookDetail.configureViewTextMeta(rr, e, header_meta);

    related_layout.setVisibility(View.GONE);

    final Simplified app = Simplified.get();
    final CatalogAcquisitionCoverCacheType cover_loader =
      app.getCatalogAcquisitionCoverLoader();

    this.loading_cover =
      cover_loader.getCoverAsynchronous(
        e,
        new BitmapDisplayHeightPreserveAspect(cover_height),
        new BitmapCacheListenerType<OPDSAcquisitionFeedEntry>() {
          @Override public void onBitmapLoadingFailure(
            final OPDSAcquisitionFeedEntry key,
            final Throwable x)
          {
            if (x instanceof CancellationException) {
              return;
            }

            Log.e(CatalogBookDetailActivity.TAG, x.getMessage(), x);
          }

          @Override public void onBitmapLoadingSuccess(
            final OPDSAcquisitionFeedEntry key,
            final Bitmap b)
          {
            Log.d(
              CatalogBookDetailActivity.TAG,
              String.format(
                "returned image is (%d x %d)",
                b.getWidth(),
                b.getHeight()));

            UIThread.runOnUIThread(new Runnable() {
              @Override public void run()
              {
                header_cover.setImageBitmap(b);
                Fade.fadeIn(header_cover, Fade.DEFAULT_FADE_DURATION);
              }
            });
          }
        });

    final FrameLayout content_area = this.getContentFrame();
    content_area.removeAllViews();
    content_area.addView(sv);
    content_area.requestLayout();
  }
}
