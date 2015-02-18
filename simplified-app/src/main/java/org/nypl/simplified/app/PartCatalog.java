package org.nypl.simplified.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.Log;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.io7m.jnull.NullCheck;

public final class PartCatalog implements PartType
{
  public static PartCatalog newInstance(
    final FragmentManager in_fm)
  {
    return new PartCatalog(in_fm);
  }

  private CatalogFeedFragment   current_fragment;
  private final EventBus        event_bus;
  private final FragmentManager fm;

  private PartCatalog(
    final FragmentManager in_fm)
  {
    this.fm = NullCheck.notNull(in_fm);

    final Simplified app = Simplified.get();
    this.event_bus = app.getCatalogEventBus();
    this.event_bus.register(this);

    this.current_fragment =
      CatalogFeedFragment.newFragmentAtRoot(app.getFeedInitialURI());
  }

  @Override public Fragment getCurrentFragment()
  {
    return this.current_fragment;
  }

  @Override public <A, E extends Exception> A matchPart(
    final PartMatcherType<A, E> m)
    throws E
  {
    return m.catalog(this);
  }

  @Subscribe public void onReceiveCatalogClickEvent(
    final CatalogNavigationClickEvent e)
  {
    Log.d("PartCatalog", "Received click event: " + e);
    this.current_fragment =
      CatalogFeedFragment.newFragment(e.getTarget(), e.getFrom());

    final FragmentTransaction ft = this.fm.beginTransaction();
    ft.replace(R.id.content_frame, this.current_fragment);
    ft.addToBackStack(null);
    ft.commit();
  }
}
