package org.nypl.simplified.app;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class CatalogNavigationFeedAdapter extends
  ArrayAdapter<OPDSNavigationFeedEntry>
{
  /**
   * Stored state for a given view.
   */

  private static final class ViewState
  {
    private final int index;
    private int       scroll_horizontal;

    ViewState(
      final int in_index)
    {
      this.index = in_index;
      this.scroll_horizontal = 0;
    }

    public int getIndex()
    {
      return this.index;
    }

    public int getScrollHorizontal()
    {
      return this.scroll_horizontal;
    }

    public void setScrollHorizontal(
      final int h)
    {
      this.scroll_horizontal = h;
    }
  }

  private static final String TAG;

  static {
    TAG = "CatalogNavigationFeedAdapter";
  }

  private static void configureEntryView(
    final OPDSNavigationFeedEntry e,
    final ViewState state,
    final Context context,
    final Map<URI, OPDSAcquisitionFeed> cached_feeds,
    final CatalogNavigationFeedTitleClickListener title_listener,
    final LinearLayout container)
  {
    final TextView title =
      NullCheck.notNull((TextView) container.findViewById(R.id.feed_title));
    final ProgressBar progress =
      NullCheck.notNull((ProgressBar) container
        .findViewById(R.id.feed_progress));
    final LinearLayout images =
      NullCheck.notNull((LinearLayout) container
        .findViewById(R.id.feed_images_linear));
    final HorizontalScrollView scroller =
      NullCheck.notNull((HorizontalScrollView) container
        .findViewById(R.id.feed_scroller));

    /**
     * Configure view to appear as if it is loading; if there is no loading to
     * be performed, the view will be set to the correct contents instantly
     * and the user should not see it.
     */

    title.setText(e.getTitle());
    title.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        title_listener.onClick(e);
      }
    });
    progress.setVisibility(View.VISIBLE);
    images.setVisibility(View.GONE);
    scroller.setScrollX(state.getScrollHorizontal());

    /**
     * Set up the featured covers.
     */

    final OptionType<URI> featured_opt = e.getFeaturedURI();
    if (featured_opt.isNone()) {
      CatalogNavigationFeedAdapter.hasNoFeaturedFeed(e, progress, images);
      return;
    }

    final Some<URI> some = (Some<URI>) featured_opt;
    final URI featured_uri = some.get();
    CatalogNavigationFeedAdapter.hasFeaturedFeed(
      e,
      context,
      progress,
      images,
      cached_feeds,
      featured_uri);
  }

  private static void feedFailed(
    final ProgressBar progress,
    final LinearLayout images)
  {
    UIThread.checkIsUIThread();

    progress.setVisibility(View.GONE);
    images.setVisibility(View.GONE);
  }

  private static void feedFailedPost(
    final ProgressBar progress,
    final LinearLayout images)
  {
    final Handler h = new Handler(Looper.getMainLooper());
    h.post(new Runnable() {
      @Override public void run()
      {
        CatalogNavigationFeedAdapter.feedFailed(progress, images);
      }
    });
  }

  private static void feedOK(
    final OPDSNavigationFeedEntry e,
    final Context context,
    final ProgressBar progress,
    final LinearLayout images,
    final URI featured_uri,
    final OPDSAcquisitionFeed feed)
  {
    UIThread.checkIsUIThread();

    final CatalogImageSet s = new CatalogImageSet(feed.getFeedEntries());
    s.configureView(context, images, new Runnable() {
      @Override public void run()
      {
        images.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
      }
    });
  }

  private static void feedOKPost(
    final OPDSNavigationFeedEntry e,
    final Context context,
    final ProgressBar progress,
    final LinearLayout images,
    final URI featured_uri,
    final OPDSAcquisitionFeed a)
  {
    final Handler h = new Handler(Looper.getMainLooper());
    h.post(new Runnable() {
      @Override public void run()
      {
        CatalogNavigationFeedAdapter.feedOK(
          e,
          context,
          progress,
          images,
          featured_uri,
          a);
      }
    });
  }

  private static void hasFeaturedFeed(
    final OPDSNavigationFeedEntry e,
    final Context context,
    final ProgressBar progress,
    final LinearLayout images,
    final Map<URI, OPDSAcquisitionFeed> cached_feeds,
    final URI featured_uri)
  {
    UIThread.checkIsUIThread();

    final String entry_id = e.getID();
    if (cached_feeds.containsKey(featured_uri)) {
      final OPDSAcquisitionFeed feed =
        NullCheck.notNull(cached_feeds.get(featured_uri));
      CatalogNavigationFeedAdapter.feedOK(
        e,
        context,
        progress,
        images,
        featured_uri,
        feed);

    } else {
      final Simplified app = Simplified.get();
      final OPDSFeedLoaderType loader = app.getFeedLoader();
      loader.fromURI(featured_uri, new OPDSFeedLoadListenerType() {
        @Override public void onFailure(
          final Exception ex)
        {
          Log.e(CatalogNavigationFeedAdapter.TAG, String.format(
            "Unable to load feed %s for entry %s",
            featured_uri,
            entry_id), ex);
          CatalogNavigationFeedAdapter.feedFailedPost(progress, images);
        }

        @Override public void onSuccess(
          final OPDSFeedType f)
        {
          f
            .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
              @Override public Unit acquisition(
                final OPDSAcquisitionFeed a)
              {
                cached_feeds.put(featured_uri, a);
                CatalogNavigationFeedAdapter.feedOKPost(
                  e,
                  context,
                  progress,
                  images,
                  featured_uri,
                  a);
                return Unit.unit();
              }

              @Override public Unit navigation(
                final OPDSNavigationFeed n)
              {
                Log.e(CatalogNavigationFeedAdapter.TAG, String.format(
                  "Expected an acquisition feed at %s for entry %s",
                  featured_uri,
                  entry_id));
                CatalogNavigationFeedAdapter.feedFailedPost(progress, images);
                return Unit.unit();
              }
            });
        }
      });
    }
  }

  private static void hasNoFeaturedFeed(
    final OPDSNavigationFeedEntry e,
    final ProgressBar progress,
    final LinearLayout images)
  {
    UIThread.checkIsUIThread();
    CatalogNavigationFeedAdapter.feedFailedPost(progress, images);
  }

  private final Map<URI, OPDSAcquisitionFeed>           cached_feeds;
  private final List<OPDSNavigationFeedEntry>           entries;
  private final ListView                                list_view;
  private final List<ViewState>                         states;
  private final CatalogNavigationFeedTitleClickListener title_listener;

  public CatalogNavigationFeedAdapter(
    final Context context,
    final ListView in_list_view,
    final List<OPDSNavigationFeedEntry> in_entries,
    final CatalogNavigationFeedTitleClickListener in_title_listener)
  {
    super(NullCheck.notNull(context), 0, NullCheck.notNull(in_entries));
    this.entries = NullCheck.notNull(in_entries);
    this.cached_feeds = new WeakHashMap<URI, OPDSAcquisitionFeed>();
    this.states = new ArrayList<ViewState>(in_entries.size());
    this.list_view = NullCheck.notNull(in_list_view);
    this.title_listener = NullCheck.notNull(in_title_listener);

    for (int index = 0; index < in_entries.size(); ++index) {
      this.states.add(new ViewState(index));
    }
  }

  @Override public View getView(
    final int position,
    final @Nullable View reused,
    final @Nullable ViewGroup parent)
  {
    final OPDSNavigationFeedEntry e =
      NullCheck.notNull(this.entries.get(position));
    final ViewState state = NullCheck.notNull(this.states.get(position));

    /**
     * Reuse the given view, or inflate a new one. Associate some state with
     * the view in order to be able to know which lane was rendered by the
     * view when it was onscreen.
     */

    final Context context = this.getContext();
    final LayoutInflater inflater =
      (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    LinearLayout container;
    if (reused != null) {
      container = (LinearLayout) NullCheck.notNull(reused);

      /**
       * If reusing a view, save the scroll position of whatever lane was
       * being rendered by that view.
       */

      final HorizontalScrollView scroller =
        NullCheck.notNull((HorizontalScrollView) container
          .findViewById(R.id.feed_scroller));
      final ViewState state_old =
        NullCheck.notNull((ViewState) container.getTag());
      final int sx = scroller.getScrollX();
      state_old.setScrollHorizontal(sx);

    } else {
      container =
        (LinearLayout) NullCheck.notNull(inflater.inflate(
          R.layout.featured_lane_view,
          parent,
          false));
    }
    container.setTag(state);

    CatalogNavigationFeedAdapter.configureEntryView(
      e,
      state,
      context,
      this.cached_feeds,
      this.title_listener,
      container);

    return container;
  }
}
