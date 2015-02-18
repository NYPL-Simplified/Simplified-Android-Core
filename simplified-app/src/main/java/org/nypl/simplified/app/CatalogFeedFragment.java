package org.nypl.simplified.app;

import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings("synthetic-access") public final class CatalogFeedFragment extends
  Fragment
{
  private static enum Status
  {
    STATUS_FAILURE,
    STATUS_LOADING,
    STATUS_LOADING_TRANSITION_FAILURE,
    STATUS_LOADING_TRANSITION_SUCCESS,
    STATUS_SUCCESS
  }

  private static final int    FADE_TIME;
  private static final String URI_BACK_ID;
  private static final String URI_START_ID;

  static {
    URI_START_ID = "org.nypl.simplified.app.PartCatalog.uri_start";
    URI_BACK_ID = "org.nypl.simplified.app.PartCatalog.uri_back";
    FADE_TIME = 1000;
  }

  static void doFadeIn(
    final View v)
  {
    v.setVisibility(View.VISIBLE);
    v.setAlpha(0.0f);

    final ViewPropertyAnimator vp = v.animate();
    vp.alpha(1.0f);
    vp.setDuration(CatalogFeedFragment.FADE_TIME);
  }

  static void doFadeOutWithRunnable(
    final View v,
    final Runnable r)
  {
    v.setVisibility(View.VISIBLE);

    final ViewPropertyAnimator vp = v.animate();
    vp.alpha(0.0f);
    vp.setDuration(CatalogFeedFragment.FADE_TIME);
    vp.withEndAction(r);
  }

  public static CatalogFeedFragment newFragment(
    final URI target,
    final URI from)
  {
    final CatalogFeedFragment f = new CatalogFeedFragment();
    final Bundle args = new Bundle();
    args.putSerializable(
      CatalogFeedFragment.URI_START_ID,
      NullCheck.notNull(target));
    args.putSerializable(CatalogFeedFragment.URI_BACK_ID, Option.some(from));
    f.setArguments(args);
    return f;
  }

  public static CatalogFeedFragment newFragmentAtRoot(
    final URI uri)
  {
    final CatalogFeedFragment f = new CatalogFeedFragment();
    final Bundle args = new Bundle();
    args.putSerializable(
      CatalogFeedFragment.URI_START_ID,
      NullCheck.notNull(uri));
    args.putSerializable(CatalogFeedFragment.URI_BACK_ID, Option.none());
    f.setArguments(args);
    return f;
  }

  private @Nullable EventBus                       catalog_bus;
  private @Nullable Throwable                      error;
  private @Nullable OPDSFeedType                   feed;
  private @Nullable ListenableFuture<OPDSFeedType> future;
  private CatalogFeedFragment.Status               status;
  private @Nullable OptionType<URI>                uri_back;
  private @Nullable URI                            uri_start;
  private @Nullable LinearLayout                   view;
  private @Nullable ListView                       view_navigation_list;
  private @Nullable ViewGroup                      view_progress_container;

  /**
   * A nullary constructor is required! Do not add parameters! Errors will
   * occur very late at runtime!
   */

  public CatalogFeedFragment()
  {
    this.status = Status.STATUS_LOADING;
  }

  private void configureViewsForError(
    final Throwable e)
  {
    Log.d("CatalogFeedFragment", "configureViewsForError: " + this);
  }

  private void configureViewsForFeed(
    final OPDSFeedType f)
  {
    Log.d("CatalogFeedFragment", "configureViewsForFeed: " + this);

    final Context ctx = NullCheck.notNull(this.getActivity());
    final ListView nl = NullCheck.notNull(this.view_navigation_list);
    final EventBus eb = NullCheck.notNull(this.catalog_bus);
    final URI start = NullCheck.notNull(this.uri_start);

    f
      .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
        @Override public Unit acquisition(
          final OPDSAcquisitionFeed af)
        {
          // TODO Auto-generated method stub
          throw new UnimplementedCodeException();
        }

        @Override public Unit navigation(
          final OPDSNavigationFeed nf)
        {
          final CatalogNavigationFeedTitleClickListener listener =
            new CatalogNavigationFeedTitleClickListener() {
              @Override public void onClick(
                final OPDSNavigationFeedEntry e)
              {
                Log.d("CatalogFeedFragment", "Clicked: " + e.getTitle());
                eb.post(new CatalogNavigationClickEvent(
                  e.getTargetURI(),
                  start));
              }
            };

          final ListAdapter adapter =
            new CatalogNavigationFeedAdapter(
              ctx,
              nl,
              nf.getFeedEntries(),
              listener);
          nl.setAdapter(adapter);
          return Unit.unit();
        }
      });
  }

  private void configureViewsVisibilityAndTransitions()
  {
    final ViewGroup p = NullCheck.notNull(this.view_progress_container);
    final ListView nl = NullCheck.notNull(this.view_navigation_list);

    p.setVisibility(View.GONE);
    nl.setVisibility(View.GONE);

    Log.d("CatalogFeedFragment", "configureViewsVisibilityAndTransitions: "
      + this.status);

    switch (this.status) {
      case STATUS_FAILURE:
      {
        p.setVisibility(View.GONE);
        break;
      }
      case STATUS_LOADING:
      {
        CatalogFeedFragment.doFadeIn(p);
        break;
      }
      case STATUS_SUCCESS:
      {
        final OPDSFeedType f = NullCheck.notNull(this.feed);
        p.setVisibility(View.GONE);
        f
          .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
            @Override public Unit acquisition(
              final OPDSAcquisitionFeed af)
            {
              throw new UnimplementedCodeException();
            }

            @Override public Unit navigation(
              final OPDSNavigationFeed nf)
            {
              CatalogFeedFragment.doFadeIn(nl);
              return Unit.unit();
            }
          });
        break;
      }
      case STATUS_LOADING_TRANSITION_FAILURE:
      {
        CatalogFeedFragment.doFadeOutWithRunnable(p, new Runnable() {
          @Override public void run()
          {
            p.setVisibility(View.GONE);
            CatalogFeedFragment.this.status = Status.STATUS_FAILURE;
          }
        });
        break;
      }
      case STATUS_LOADING_TRANSITION_SUCCESS:
      {
        final OPDSFeedType f = NullCheck.notNull(this.feed);

        CatalogFeedFragment.doFadeOutWithRunnable(p, new Runnable() {
          @Override public void run()
          {
            p.setVisibility(View.GONE);

            f
              .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
                @Override public Unit acquisition(
                  final OPDSAcquisitionFeed af)
                {
                  throw new UnimplementedCodeException();
                }

                @Override public Unit navigation(
                  final OPDSNavigationFeed nf)
                {
                  CatalogFeedFragment.doFadeIn(nl);
                  CatalogFeedFragment.this.status = Status.STATUS_SUCCESS;
                  return Unit.unit();
                }
              });
          }
        });
        break;
      }
    }
  }

  @Override public void onAttach(
    final @Nullable Activity activity)
  {
    super.onAttach(activity);
    Log.d("CatalogFeedFragment", "onAttach: " + this);
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d("CatalogFeedFragment", "onCreate: " + this);

    final Bundle args = this.getArguments();
    final URI in_uri_start =
      NullCheck.notNull((URI) args
        .getSerializable(CatalogFeedFragment.URI_START_ID));
    final OptionType<URI> in_uri_back =
      NullCheck.notNull((OptionType<URI>) args
        .getSerializable(CatalogFeedFragment.URI_BACK_ID));

    this.uri_start = in_uri_start;
    this.uri_back = in_uri_back;

    final Simplified app = Simplified.get();
    this.catalog_bus = app.getCatalogEventBus();

    final OPDSFeedLoaderType loader = app.getFeedLoader();
    this.future =
      loader.fromURI(in_uri_start, new OPDSFeedLoadListenerType() {
        @Override public void onFailure(
          final Throwable e)
        {
          CatalogFeedFragment.this.onReceiveFeedFailure(e);
        }

        @Override public void onSuccess(
          final OPDSFeedType f)
        {
          CatalogFeedFragment.this.onReceiveFeedSuccess(f);
        }
      });
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    Log.d("CatalogFeedFragment", "onCreateView: " + this);

    assert inflater != null;

    final LinearLayout v =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog,
        container,
        false));
    final ViewGroup p =
      NullCheck.notNull((ViewGroup) v
        .findViewById(R.id.catalog_progress_container));
    final ListView nl =
      NullCheck
        .notNull((ListView) v.findViewById(R.id.catalog_nav_feed_list));

    this.view = v;
    this.view_progress_container = p;
    this.view_navigation_list = nl;

    switch (this.status) {
      case STATUS_FAILURE:
      case STATUS_LOADING_TRANSITION_FAILURE:
      {
        break;
      }
      case STATUS_SUCCESS:
      case STATUS_LOADING_TRANSITION_SUCCESS:
      {
        this.configureViewsForFeed(NullCheck.notNull(this.feed));
        break;
      }
      case STATUS_LOADING:
      {
        break;
      }
    }

    this.configureViewsVisibilityAndTransitions();
    return v;
  }

  @Override public void onDestroy()
  {
    super.onDestroy();
    Log.d("CatalogFeedFragment", "onDestroy: " + this);

    final ListenableFuture<OPDSFeedType> f = this.future;
    if (f != null) {
      if (f.isDone() == false) {
        f.cancel(true);
      }
    }
  }

  @Override public void onDestroyView()
  {
    super.onDestroyView();
    Log.d("CatalogFeedFragment", "onDestroyView: " + this);
  }

  @Override public void onDetach()
  {
    super.onDetach();
    Log.d("CatalogFeedFragment", "onDetach: " + this);
  }

  private void onReceiveFeedFailure(
    final Throwable e)
  {
    this.status = Status.STATUS_LOADING_TRANSITION_FAILURE;
    this.error = e;

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        CatalogFeedFragment.this.configureViewsForError(e);
        CatalogFeedFragment.this.configureViewsVisibilityAndTransitions();
      }
    });
  }

  private void onReceiveFeedSuccess(
    final OPDSFeedType f)
  {
    this.status = Status.STATUS_LOADING_TRANSITION_SUCCESS;
    this.feed = f;

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        CatalogFeedFragment.this.configureViewsForFeed(f);
        CatalogFeedFragment.this.configureViewsVisibilityAndTransitions();
      }
    });
  }

  @Override public void onResume()
  {
    super.onResume();
    Log.d("CatalogFeedFragment", "onResume: " + this);
  }
}
