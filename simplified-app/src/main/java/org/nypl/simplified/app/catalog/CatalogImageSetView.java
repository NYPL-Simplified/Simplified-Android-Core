package org.nypl.simplified.app.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.nypl.simplified.app.CoverProviderType;
import org.nypl.simplified.app.ExpensiveStoppableType;
import org.nypl.simplified.app.ScreenSizeControllerType;
import org.nypl.simplified.app.utilities.FadeUtilities;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.squareup.picasso.Callback;

@SuppressWarnings("synthetic-access") public final class CatalogImageSetView extends
  LinearLayout implements ExpensiveStoppableType
{
  private static final Logger                  LOG;

  static {
    LOG = LogUtilities.getLog(CatalogImageSetView.class);
  }

  private final CoverProviderType              cover_provider;
  private final Runnable                       done_proc;
  private final List<OPDSAcquisitionFeedEntry> entries;
  private final String                         id;
  private final int                            image_height;
  private final List<ImageView>                imageviews;

  public CatalogImageSetView(
    final Context in_context,
    final ScreenSizeControllerType in_screen,
    final CoverProviderType in_cover_provider,
    final CatalogNavigationLaneView in_lane,
    final List<OPDSAcquisitionFeedEntry> in_entries,
    final CatalogNavigationLaneViewListenerType in_listener,
    final int in_image_height,
    final String in_id,
    final Runnable in_done)
  {
    super(NullCheck.notNull(in_context));

    UIThread.checkIsUIThread();

    NullCheck.notNull(in_screen);
    NullCheck.notNull(in_lane);
    this.entries = NullCheck.notNull(in_entries);
    NullCheck.notNull(in_listener);
    this.id = NullCheck.notNull(in_id);
    this.done_proc = NullCheck.notNull(in_done);
    this.cover_provider = NullCheck.notNull(in_cover_provider);

    this.imageviews = new ArrayList<ImageView>();
    this.image_height = in_image_height;
    this.setOrientation(LinearLayout.HORIZONTAL);

    final LayoutParams p =
      new LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT);
    this.setLayoutParams(p);
    this.setVisibility(View.INVISIBLE);

    final AtomicInteger done_count = new AtomicInteger(0);
    for (int index = 0; index < in_entries.size(); ++index) {
      final OPDSAcquisitionFeedEntry e =
        NullCheck.notNull(this.entries.get(index));

      final ImageView i = new ImageView(in_context);
      i.setOnClickListener(new OnClickListener() {
        @Override public void onClick(
          final @Nullable View v)
        {
          in_listener.onSelectBook(in_lane, e);
        }
      });

      final int h = in_image_height;
      final int w = (int) (h * 0.75);

      final Callback cover_callback = new Callback() {
        @Override public void onError()
        {
          if (done_count.incrementAndGet() >= in_entries.size()) {
            CatalogImageSetView.this.done();
          }
        }

        @Override public void onSuccess()
        {
          if (done_count.incrementAndGet() >= in_entries.size()) {
            CatalogImageSetView.this.done();
          }
        }
      };

      this.cover_provider.loadThumbnailIntoWithCallback(
        e,
        i,
        w,
        h,
        cover_callback);

      this.imageviews.add(i);
      this.addView(i);

      final View spacer = new View(in_context);
      final android.view.ViewGroup.LayoutParams q =
        new LayoutParams(
          (int) in_screen.screenDPToPixels(8),
          this.image_height);
      spacer.setLayoutParams(q);
      spacer.setVisibility(View.INVISIBLE);
      this.addView(spacer);
    }
  }

  private void done()
  {
    CatalogImageSetView.LOG.debug("{}: images done", this.id);

    final CatalogImageSetView sv = this;
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        FadeUtilities.fadeIn(sv, FadeUtilities.DEFAULT_FADE_DURATION);
        sv.done_proc.run();
      }
    });
  }

  @Override public void expensiveStop()
  {
    CatalogImageSetView.LOG.debug("{}: images cancelled", this.id);
  }
}
