package org.nypl.simplified.app;

import java.net.URI;
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
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings("synthetic-access") public final class CatalogNavigationFeedAdapter extends
  ArrayAdapter<OPDSNavigationFeedEntry>
{
  private static final String TAG;
  static {
    TAG = "CatalogNavigationFeedAdapter";
  }

  private static void configureEntryView(
    final OPDSNavigationFeedEntry e,
    final Context context,
    final Map<URI, OPDSAcquisitionFeed> cached_feeds,
    final LinearLayout container)
  {
    final TextView title = (TextView) container.findViewById(R.id.feed_title);
    title.setText(e.getTitle());

    final ProgressBar progress =
      (ProgressBar) container.findViewById(R.id.feed_progress);
    progress.setVisibility(View.VISIBLE);

    final LinearLayout images =
      (LinearLayout) container.findViewById(R.id.feed_images_linear);
    images.removeAllViews();
    images.setVisibility(View.VISIBLE);

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
    progress.setVisibility(View.GONE);
    images.setVisibility(View.GONE);
    images.removeAllViews();
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
    final CatalogImageSet s = new CatalogImageSet(feed.getFeedEntries());
    s.configureView(context, images, new Runnable() {
      @Override public void run()
      {
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
    final String entry_id = e.getID();
    Log.d(
      CatalogNavigationFeedAdapter.TAG,
      String.format("Entry %s has no featured feed", entry_id));
    CatalogNavigationFeedAdapter.feedFailedPost(progress, images);
  }

  private final Map<URI, OPDSAcquisitionFeed> cached_feeds;
  private final List<OPDSNavigationFeedEntry> entries;

  public CatalogNavigationFeedAdapter(
    final Context context,
    final List<OPDSNavigationFeedEntry> in_entries)
  {
    super(NullCheck.notNull(context), 0, NullCheck.notNull(in_entries));
    this.entries = NullCheck.notNull(in_entries);
    this.cached_feeds = new WeakHashMap<URI, OPDSAcquisitionFeed>();
  }

  @Override public View getView(
    final int position,
    final @Nullable View reused,
    final @Nullable ViewGroup parent)
  {
    final OPDSNavigationFeedEntry e =
      NullCheck.notNull(this.entries.get(position));

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

    CatalogNavigationFeedAdapter.configureEntryView(
      e,
      context,
      this.cached_feeds,
      container);
    return container;
  }
}
