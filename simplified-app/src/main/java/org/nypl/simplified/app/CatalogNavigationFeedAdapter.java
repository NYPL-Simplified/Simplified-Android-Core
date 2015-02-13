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
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
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
   * A {@link RecyclerListener} that saves the positions of scroll views as
   * they move offscreen.
   */

  private static final class ScrollViewSaver implements RecyclerListener
  {
    @Override public void onMovedToScrapHeap(
      final @Nullable View view)
    {
      assert view != null;
      if (view instanceof LinearLayout) {
        final LinearLayout ll = (LinearLayout) view;
        final Object state_raw = ll.getTag();
        if ((state_raw != null) && (state_raw instanceof ViewState)) {
          final ViewState state = (ViewState) state_raw;
          final HorizontalScrollView scroller =
            (HorizontalScrollView) ll.findViewById(R.id.feed_scroller);
          CatalogNavigationFeedAdapter.scrollViewSaveState(state, scroller);
        }
      }
    }
  }

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
    progress.setVisibility(View.VISIBLE);
    images.setVisibility(View.GONE);
    CatalogNavigationFeedAdapter.scrollViewRestoreState(state, scroller);

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
    Log.d(
      CatalogNavigationFeedAdapter.TAG,
      String.format("Entry %s has featured feed %s", entry_id, featured_uri));

    if (cached_feeds.containsKey(featured_uri)) {
      Log.d(CatalogNavigationFeedAdapter.TAG, String.format(
        "Got cached acquisition feed %s for entry %s",
        featured_uri,
        entry_id));

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
                Log.d(CatalogNavigationFeedAdapter.TAG, String.format(
                  "Got acquisition feed %s for entry %s",
                  featured_uri,
                  entry_id));

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

    final String entry_id = e.getID();
    Log.d(
      CatalogNavigationFeedAdapter.TAG,
      String.format("Entry %s has no featured feed", entry_id));
    CatalogNavigationFeedAdapter.feedFailedPost(progress, images);
  }

  /**
   * Restore the scroll position of the given scroll view from the given
   * state.
   */

  private static void scrollViewRestoreState(
    final ViewState state,
    final HorizontalScrollView scroller)
  {
    final int sx = state.getScrollHorizontal();
    Log.d(CatalogNavigationFeedAdapter.TAG, String.format(
      "restoring scroll position %d for scroll view %d",
      sx,
      state.getIndex()));
    scroller.setScrollX(sx);
  }

  /**
   * Save the scroll position of the given scroll view to the given state.
   */

  private static void scrollViewSaveState(
    final ViewState state,
    final HorizontalScrollView scroller)
  {
    final int sx = scroller.getScrollX();
    Log.d(
      CatalogNavigationFeedAdapter.TAG,
      String.format(
        "saving scroll position %d for scroll view %d",
        sx,
        state.getIndex()));
    state.setScrollHorizontal(sx);
  }

  private final Map<URI, OPDSAcquisitionFeed> cached_feeds;
  private final List<OPDSNavigationFeedEntry> entries;
  private final ListView                      list_view;
  private final List<ViewState>               states;

  public CatalogNavigationFeedAdapter(
    final Context context,
    final ListView in_list_view,
    final List<OPDSNavigationFeedEntry> in_entries)
  {
    super(NullCheck.notNull(context), 0, NullCheck.notNull(in_entries));
    this.entries = NullCheck.notNull(in_entries);
    this.cached_feeds = new WeakHashMap<URI, OPDSAcquisitionFeed>();
    this.states = new ArrayList<ViewState>(in_entries.size());
    this.list_view = NullCheck.notNull(in_list_view);

    for (int index = 0; index < in_entries.size(); ++index) {
      this.states.add(new ViewState(index));
    }

    /**
     * Register a listener to save the scroll position of the scroll view in
     * each lane whenever that lane is moved offscreen.
     */

    this.list_view.setRecyclerListener(new ScrollViewSaver());
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
      container);

    return container;
  }
}
