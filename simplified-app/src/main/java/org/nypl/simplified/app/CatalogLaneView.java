package org.nypl.simplified.app;

import java.net.URI;
import java.util.concurrent.CancellationException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings("synthetic-access") public final class CatalogLaneView extends
  LinearLayout implements CancellableType
{
  private static final class UnimplementedListener implements
    CatalogLaneViewListenerType
  {
    public UnimplementedListener()
    {

    }

    @Override public void onSelectBook(
      final CatalogLaneView v,
      final OPDSAcquisitionFeedEntry e)
    {
      throw new UnimplementedCodeException();
    }

    @Override public void onSelectFeed(
      final CatalogLaneView v,
      final OPDSNavigationFeedEntry feed)
    {
      throw new UnimplementedCodeException();
    }
  }

  private static final String                      TAG;

  static {
    TAG = "CLane";
  }

  private @Nullable OPDSNavigationFeedEntry        entry;
  private final RelativeLayout                     header;
  private @Nullable CatalogImageSet                image_set;
  private final LinearLayout                       images;
  private CatalogLaneViewListenerType              listener;
  private @Nullable ListenableFuture<OPDSFeedType> loading;
  private final ProgressBar                        progress;
  private final HorizontalScrollView               scroller;
  private final TextView                           title;

  public CatalogLaneView(
    final Context context)
  {
    this(context, null);
  }

  public CatalogLaneView(
    final Context context,
    final @Nullable AttributeSet attrs)
  {
    super(context, attrs);
    this.listener = new UnimplementedListener();

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
    this.images =
      NullCheck.notNull((LinearLayout) this
        .findViewById(R.id.feed_images_linear));

    this.scroller.setHorizontalScrollBarEnabled(false);
  }

  @Override public void cancel()
  {
    Log.d(CatalogLaneView.TAG, "Cancelling CatalogLaneView");
    final ListenableFuture<OPDSFeedType> f = this.loading;
    if (f != null) {
      f.cancel(true);
    }
  }

  private void onAcquisitionFeedReceived(
    final OPDSAcquisitionFeed af)
  {
    final CatalogImageSet is = new CatalogImageSet(af.getFeedEntries());
    final LinearLayout closure_images = this.images;
    final ProgressBar closure_progress = this.progress;
    final Context ctx = NullCheck.notNull(this.getContext());
    final CatalogLaneView closure_this = this;
    final CatalogLaneViewListenerType closure_listener = this.listener;

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        is.configureView(
          ctx,
          closure_this,
          closure_listener,
          closure_images,
          new Runnable() {
            @Override public void run()
            {
              Log.d(CatalogLaneView.TAG, "images loaded");
              closure_images.setVisibility(View.VISIBLE);
              closure_progress.setVisibility(View.GONE);
            }
          });
      }
    });

    this.image_set = is;
  }

  private void onFeedFailed()
  {
    this.scroller.setVisibility(View.GONE);
    this.progress.setVisibility(View.GONE);
  }

  private void onNavigationFeedReceived()
  {
    Log.e(
      CatalogLaneView.TAG,
      "Expected an acquisition feed but received a navigation feed");
    CatalogLaneView.this.onFeedFailed();
  }

  public void setLaneViewFeedAndListener(
    final OPDSNavigationFeedEntry e,
    final CatalogLaneViewListenerType v)
  {
    this.entry = NullCheck.notNull(e);
    this.listener = NullCheck.notNull(v);

    this.title.setText(e.getTitle());
    this.title.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View view_title)
      {
        v.onSelectFeed(CatalogLaneView.this, e);
      }
    });

    final OptionType<URI> featured_opt = e.getFeaturedURI();
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
            CatalogLaneView.this.onFeedFailed();
          }

          @Override public void onSuccess(
            final OPDSFeedType f)
          {
            f
              .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
                @Override public Unit acquisition(
                  final OPDSAcquisitionFeed af)
                {
                  CatalogLaneView.this.onAcquisitionFeedReceived(af);
                  return Unit.unit();
                }

                @Override public Unit navigation(
                  final OPDSNavigationFeed nf)
                {
                  CatalogLaneView.this.onNavigationFeedReceived();
                  return Unit.unit();
                }
              });
          }
        });
    } else {
      this.onFeedFailed();
    }
  }
}
