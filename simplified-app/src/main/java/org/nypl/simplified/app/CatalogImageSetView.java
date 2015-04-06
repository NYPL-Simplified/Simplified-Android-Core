package org.nypl.simplified.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

@SuppressWarnings("synthetic-access") public final class CatalogImageSetView extends
  LinearLayout implements ExpensiveStoppableType
{
  private static final String                  TAG;

  static {
    TAG = "CImagesView";
  }

  private final Runnable                       done_proc;
  private final List<OPDSAcquisitionFeedEntry> entries;
  private final String                         id;
  private final int                            image_height;
  private final List<ImageView>                imageviews;
  private final Picasso                        picasso;

  public CatalogImageSetView(
    final Context in_context,
    final ScreenSizeControllerType in_screen,
    final Picasso in_picasso,
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
    this.picasso = NullCheck.notNull(in_picasso);

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

      PicassoUtilities.loadThumbnailIntoWithCallback(
        in_picasso,
        e,
        i,
        w,
        h,
        new Callback() {
          @Override public void onSuccess()
          {
            if (done_count.incrementAndGet() >= in_entries.size()) {
              CatalogImageSetView.this.done();
            }
          }

          @Override public void onError()
          {
            if (done_count.incrementAndGet() >= in_entries.size()) {
              CatalogImageSetView.this.done();
            }
          }
        });

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
    Log.d(CatalogImageSetView.TAG, this.id + ": images done");

    final CatalogImageSetView sv = this;
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        Fade.fadeIn(sv, Fade.DEFAULT_FADE_DURATION);
        sv.done_proc.run();
      }
    });
  }

  @Override public void expensiveStop()
  {
    Log.d(CatalogImageSetView.TAG, this.id + ": images cancelled");
  }
}
