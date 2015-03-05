package org.nypl.simplified.app;

import java.net.URI;
import java.util.concurrent.CancellationException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings("synthetic-access") public final class CatalogLaneView extends
  LinearLayout
{
  private static final String                               TAG;

  static {
    TAG = "CLane";
  }

  private final OPDSNavigationFeedEntry                     entry;
  private @Nullable OPDSAcquisitionFeed                     feed_received;
  private final RelativeLayout                              header;
  private volatile @Nullable CatalogImageSetView            images;
  private final CatalogLaneViewListenerType                 listener;
  private volatile @Nullable ListenableFuture<OPDSFeedType> loading;
  private final ProgressBar                                 progress;
  private final HorizontalScrollView                        scroller;
  private final TextView                                    title;
  private int                                               scroller_position;

  public CatalogLaneView(
    final Context context,
    final @Nullable AttributeSet attrs,
    final OPDSNavigationFeedEntry e,
    final CatalogLaneViewListenerType in_listener)
  {
    super(context, attrs);

    this.entry = NullCheck.notNull(e);
    this.listener = NullCheck.notNull(in_listener);

    this.setOrientation(LinearLayout.VERTICAL);

    final LayoutInflater inflater =
      (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.lane_view, this, true);

    this.header =
      NullCheck.notNull((RelativeLayout) this.findViewById(R.id.feed_header));
    this.title =
      NullCheck.notNull((TextView) this.findViewById(R.id.feed_title));
    this.progress =
      NullCheck.notNull((ProgressBar) this.findViewById(R.id.feed_progress));
    this.scroller =
      NullCheck.notNull((HorizontalScrollView) this
        .findViewById(R.id.feed_scroller));
    this.scroller.setHorizontalScrollBarEnabled(false);

    this.title.setText(e.getTitle());
    this.title.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View view_title)
      {
        in_listener.onSelectFeed(CatalogLaneView.this, e);
      }
    });
  }

  public CatalogLaneView(
    final Context context,
    final OPDSNavigationFeedEntry e,
    final CatalogLaneViewListenerType in_listener)
  {
    this(context, null, e, in_listener);
  }

  public void laneViewRequestDisplay()
  {
    UIThread.checkIsUIThread();

    Log.d(CatalogLaneView.TAG, "request start displaying");

    this.progress.setVisibility(View.VISIBLE);
    final OPDSAcquisitionFeed fr = this.feed_received;
    if (fr != null) {
      this.onAcquisitionFeedReceived(fr);
    } else {
      this.loadFeed();
    }
  }

  public void laneViewRequestStopDisplaying()
  {
    UIThread.checkIsUIThread();

    Log.d(CatalogLaneView.TAG, "request stop displaying");
    this.scroller_position = this.scroller.getScrollX();

    if (this.images != null) {
      this.images.cancel();
    }

    this.scroller.setVisibility(View.INVISIBLE);
    this.scroller.removeAllViews();
  }

  private void loadFeed()
  {
    /**
     * If there is already a loading operating in progress, do not start
     * another.
     */

    final ListenableFuture<OPDSFeedType> lf = this.loading;
    if (lf != null) {
      if ((lf.isCancelled() == false) && (lf.isDone() == false)) {
        return;
      }

      /**
       * Otherwise, try again...
       */
    }

    /**
     * Otherwise, load the acquisition feed associated with the entry.
     */

    final OptionType<URI> featured_opt = this.entry.getFeaturedURI();
    if (featured_opt.isSome()) {
      final Some<URI> some = (Some<URI>) featured_opt;
      final Simplified app = Simplified.get();
      final OPDSFeedLoaderType loader = app.getFeedLoader();
      this.loading =
        loader.fromURI(some.get(), new OPDSFeedLoadListenerType() {
          @Override public void onFailure(
            final Throwable ex)
          {
            if (ex instanceof CancellationException) {
              Log.d(CatalogLaneView.TAG, "Loading cancelled");
              return;
            }

            Log.e(
              CatalogLaneView.TAG,
              "Failed to load featured feed: " + ex.getMessage(),
              ex);

            UIThread.runOnUIThread(new Runnable() {
              @Override public void run()
              {
                CatalogLaneView.this.onFeedFailed();
              }
            });
          }

          @Override public void onSuccess(
            final OPDSFeedType f)
          {
            f
              .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
                @Override public Unit acquisition(
                  final OPDSAcquisitionFeed af)
                {
                  UIThread.runOnUIThread(new Runnable() {
                    @Override public void run()
                    {
                      CatalogLaneView.this.onAcquisitionFeedReceived(af);
                    }
                  });
                  return Unit.unit();
                }

                @Override public Unit navigation(
                  final OPDSNavigationFeed nf)
                {
                  UIThread.runOnUIThread(new Runnable() {
                    @Override public void run()
                    {
                      CatalogLaneView.this.onNavigationFeedReceived();
                    }
                  });
                  return Unit.unit();
                }
              });
          }
        });
    } else {
      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          CatalogLaneView.this.onFeedFailed();
        }
      });
    }
  }

  private void onAcquisitionFeedReceived(
    final OPDSAcquisitionFeed af)
  {
    UIThread.checkIsUIThread();

    this.feed_received = NullCheck.notNull(af);

    final Simplified app = Simplified.get();
    final int scroll_x = this.scroller_position;
    final HorizontalScrollView cs = this.scroller;
    final ProgressBar cp = this.progress;
    final Context ctx = NullCheck.notNull(this.getContext());

    final android.view.ViewGroup.LayoutParams slp = cs.getLayoutParams();
    Preconditions.checkArgument(slp.height > 0);

    final CatalogImageSetView i =
      new CatalogImageSetView(
        ctx,
        this,
        app.getListeningExecutorService(),
        app.getCatalogAcquisitionThumbnailLoader(),
        af.getFeedEntries(),
        this.listener,
        slp.height,
        af.getFeedID(),
        new Runnable() {
          @Override public void run()
          {
            /*
             * XXX: This is not the correct place to set the scroll position,
             * unfortunately. The position (apparently) needs to be set when
             * the view contained within the scroll view has been measured
             * (which won't occur until the scroll view is visible).
             */

            cs.setScrollX(scroll_x);
            cp.setVisibility(View.INVISIBLE);
          }
        });

    cs.removeAllViews();
    cs.addView(i);
    cs.setVisibility(View.VISIBLE);
    this.images = i;
  }

  private void onFeedFailed()
  {
    UIThread.checkIsUIThread();
    this.scroller.setVisibility(View.INVISIBLE);
    this.progress.setVisibility(View.INVISIBLE);
  }

  private void onNavigationFeedReceived()
  {
    UIThread.checkIsUIThread();
    Log.e(
      CatalogLaneView.TAG,
      "Expected an acquisition feed but received a navigation feed");
    CatalogLaneView.this.onFeedFailed();
  }
}
