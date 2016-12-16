package org.nypl.simplified.app.catalog;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import com.squareup.picasso.Callback;
import org.nypl.simplified.app.BookCoverProviderType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.ScreenSizeControllerType;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.utilities.FadeUtilities;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.books.core.FeedEntryCorrupt;
import org.nypl.simplified.books.core.FeedEntryMatcherType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.FeedEntryType;
import org.nypl.simplified.books.core.FeedGroup;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.multilibrary.AccountsRegistry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.prefs.Prefs;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A feed lane.
 */

@SuppressWarnings("synthetic-access") public final class CatalogFeedLane
  extends LinearLayout
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogFeedLane.class);
  }

  private final BookCoverProviderType       covers;
  private final int                         image_height;
  private final CatalogFeedLaneListenerType listener;
  private final ProgressBar                 progress;
  private final ScreenSizeControllerType    screen;
  private final HorizontalScrollView        scroller;
  private final ViewGroup                   scroller_contents;
  private final TextView                    title;
  private final RelativeLayout              header;

  /**
   * Construct a feed lane.
   *
   * @param in_context  A context
   * @param in_covers   A cover provider
   * @param in_screen   The screen
   * @param in_listener A lane listener
   */

  public CatalogFeedLane(
    final Context in_context,
    final BookCoverProviderType in_covers,
    final ScreenSizeControllerType in_screen,
    final CatalogFeedLaneListenerType in_listener)
  {
    super(NullCheck.notNull(in_context));
    this.covers = NullCheck.notNull(in_covers);
    this.screen = NullCheck.notNull(in_screen);
    this.listener = NullCheck.notNull(in_listener);

    this.setOrientation(LinearLayout.VERTICAL);

    final LayoutInflater inflater =
      (LayoutInflater) in_context.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.catalog_feed_groups_lane, this, true);



    Account account =   Simplified.getCurrentAccount();

    this.header =
      NullCheck.notNull((RelativeLayout) this.findViewById(R.id.feed_header));
    ((TextView)this.header.findViewById(R.id.feed_title)).setTextColor(Color.parseColor(account.getMainColor()));
    ((TextView)this.header.findViewById(R.id.feed_more)).setTextColor(Color.parseColor(account.getMainColor()));

    this.title =
      NullCheck.notNull((TextView) this.findViewById(R.id.feed_title));
    this.progress =
      NullCheck.notNull((ProgressBar) this.findViewById(R.id.feed_progress));
    this.scroller = NullCheck.notNull(
      (HorizontalScrollView) this.findViewById(R.id.feed_scroller));
    this.scroller.setHorizontalScrollBarEnabled(false);

    this.scroller_contents = NullCheck.notNull(
      (ViewGroup) this.scroller.findViewById(R.id.feed_scroller_contents));

    final android.view.ViewGroup.LayoutParams sp =
      this.scroller.getLayoutParams();
    this.image_height = sp.height;
  }

  /**
   * Configure the lane for the given group.
   *
   * @param in_group The group
   */

  public void configureForGroup(
    final FeedGroup in_group)
  {
    NullCheck.notNull(in_group);
    this.configureView(in_group);
  }

  private void configureView(
    final FeedGroup in_group)
  {
    this.scroller.setVisibility(View.INVISIBLE);
    this.scroller.post(
      new Runnable()
      {
        @Override public void run()
        {
          CatalogFeedLane.this.scroller.scrollTo(0, 0);
        }
      });

    this.scroller_contents.setVisibility(View.INVISIBLE);
    this.progress.setVisibility(View.VISIBLE);

    this.scroller_contents.removeAllViews();

    this.title.setText(in_group.getGroupTitle());

    final Resources rr = NullCheck.notNull(this.getResources());

    this.header.setContentDescription(String.format(rr.getString(R.string.catalog_accessibility_header_show_more), this.title.getText()));
    this.header.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View view_title)
        {
          CatalogFeedLane.this.listener.onSelectFeed(in_group);
        }
      });

    final List<FeedEntryType> es =
      NullCheck.notNull(in_group.getGroupEntries());
    final ArrayList<ImageView> image_views =
      new ArrayList<ImageView>(es.size());

    for (int index = 0; index < es.size(); ++index) {
      final FeedEntryType e = NullCheck.notNull(es.get(index));
      e.matchFeedEntry(
        new FeedEntryMatcherType<Unit, UnreachableCodeException>()
        {
          @Override public Unit onFeedEntryCorrupt(
            final FeedEntryCorrupt ec)
          {
            image_views.add(null);
            return Unit.unit();
          }

          @Override public Unit onFeedEntryOPDS(
            final FeedEntryOPDS eo)
          {
            final ImageView image_view =
              new ImageView(CatalogFeedLane.this.getContext());

            final LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
              android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
              android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(
              0, 0, (int) CatalogFeedLane.this.screen.screenDPToPixels(8), 0);
            image_view.setLayoutParams(p);
            final OPDSAcquisitionFeedEntry eoe = eo.getFeedEntry();
            image_view.setContentDescription(eoe.getTitle());
            image_view.setOnClickListener(
              new OnClickListener()
              {
                @Override public void onClick(
                  final @Nullable View v)
                {
                  CatalogFeedLane.this.listener.onSelectBook(eo);
                }
              });

            image_views.add(image_view);
            CatalogFeedLane.this.scroller_contents.addView(image_view);
            return Unit.unit();
          }
        });
    }

    final int image_width =
      (int) ((double) CatalogFeedLane.this.image_height * 0.75);
    final AtomicInteger images_left = new AtomicInteger(es.size());
    for (int index = 0; index < es.size(); ++index) {
      final ImageView image_view = image_views.get(index);
      if (image_view == null) {
        continue;
      }

      final FeedEntryType e = NullCheck.notNull(es.get(index));
      final Callback cover_callback = new Callback()
      {
        @Override public void onError()
        {
          CatalogFeedLane.LOG.debug(
            "could not load image for {}", e.getBookID());

          image_view.setVisibility(View.GONE);
          if (images_left.decrementAndGet() <= 0) {
            CatalogFeedLane.this.done();
          }
        }

        @Override public void onSuccess()
        {
          if (images_left.decrementAndGet() <= 0) {
            CatalogFeedLane.this.done();
          }
        }
      };

      this.covers.loadThumbnailIntoWithCallback(
        (FeedEntryOPDS) e,
        image_view,
        image_width,
        this.image_height,
        cover_callback);
    }
  }

  private void done()
  {
    CatalogFeedLane.LOG.debug("images done");

    this.progress.setVisibility(View.INVISIBLE);
    this.scroller_contents.setVisibility(View.VISIBLE);
    FadeUtilities.fadeIn(this.scroller, FadeUtilities.DEFAULT_FADE_DURATION);
  }
}
