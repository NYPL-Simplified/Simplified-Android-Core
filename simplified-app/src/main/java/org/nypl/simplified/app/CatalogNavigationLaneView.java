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

@SuppressWarnings("synthetic-access") public final class CatalogNavigationLaneView extends
  LinearLayout implements ExpensiveType
{
  private static final String                               TAG;

  static {
    TAG = "CLane";
  }

  private final OPDSNavigationFeedEntry                     entry;
  private @Nullable OPDSAcquisitionFeed                     feed_received;
  private final RelativeLayout                              header;
  private volatile @Nullable CatalogImageSetView            images;
  private final CatalogNavigationLaneViewListenerType       listener;
  private volatile @Nullable ListenableFuture<OPDSFeedType> loading;
  private final ProgressBar                                 progress;
  private final HorizontalScrollView                        scroller;
  private int                                               scroller_position;
  private final TextView                                    title;

  public CatalogNavigationLaneView(
    final Context context,
    final @Nullable AttributeSet attrs,
    final ScreenSizeControllerType screen,
    final OPDSNavigationFeedEntry e,
    final CatalogNavigationLaneViewListenerType in_listener)
  {
    super(context, attrs);

    NullCheck.notNull(screen);
    this.entry = NullCheck.notNull(e);
    this.listener = NullCheck.notNull(in_listener);

    this.setOrientation(LinearLayout.VERTICAL);

    final LayoutInflater inflater =
      (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.catalog_navigation_lane_view, this, true);

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
        in_listener.onSelectFeed(CatalogNavigationLaneView.this, e);
      }
    });
  }

  public CatalogNavigationLaneView(
    final Context context,
    final ScreenSizeControllerType screen,
    final OPDSNavigationFeedEntry e,
    final CatalogNavigationLaneViewListenerType in_listener)
  {
    this(context, null, screen, e, in_listener);
  }

  @Override public void expensiveStart()
  {
    UIThread.checkIsUIThread();

    Log.d(CatalogNavigationLaneView.TAG, "request start displaying");

    this.progress.setVisibility(View.VISIBLE);
    final OPDSAcquisitionFeed fr = this.feed_received;
    if (fr != null) {
      this.onAcquisitionFeedReceived(fr);
    } else {
      this.loadFeed();
    }
  }

  @Override public void expensiveStop()
  {
    UIThread.checkIsUIThread();

    Log.d(CatalogNavigationLaneView.TAG, "request stop displaying");
    this.scroller_position = this.scroller.getScrollX();

    if (this.images != null) {
      this.images.expensiveStop();
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
          @Override public void onFeedLoadingFailure(
            final Throwable ex)
          {
            if (ex instanceof CancellationException) {
              Log.d(CatalogNavigationLaneView.TAG, "Loading cancelled");
              return;
            }

            Log.e(
              CatalogNavigationLaneView.TAG,
              "Failed to load featured feed: " + ex.getMessage(),
              ex);

            UIThread.runOnUIThread(new Runnable() {
              @Override public void run()
              {
                CatalogNavigationLaneView.this.onFeedFailed();
              }
            });
          }

          @Override public void onFeedLoadingSuccess(
            final OPDSFeedType f)
          {
            f
              .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
                @Override public Unit onAcquisitionFeed(
                  final OPDSAcquisitionFeed af)
                {
                  UIThread.runOnUIThread(new Runnable() {
                    @Override public void run()
                    {
                      CatalogNavigationLaneView.this
                        .onAcquisitionFeedReceived(af);
                    }
                  });
                  return Unit.unit();
                }

                @Override public Unit onNavigationFeed(
                  final OPDSNavigationFeed nf)
                {
                  UIThread.runOnUIThread(new Runnable() {
                    @Override public void run()
                    {
                      CatalogNavigationLaneView.this
                        .onNavigationFeedReceived();
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
          CatalogNavigationLaneView.this.onFeedFailed();
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
        app,
        this,
        app.getListeningExecutorService(),
        app.getCatalogThumbnailLoader(),
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
      CatalogNavigationLaneView.TAG,
      "Expected an acquisition feed but received a navigation feed");
    CatalogNavigationLaneView.this.onFeedFailed();
  }
}
