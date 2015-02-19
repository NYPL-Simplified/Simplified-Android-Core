package org.nypl.simplified.app;

import java.net.URI;
import java.util.ArrayDeque;

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

    final ArrayDeque<URI> stack = new ArrayDeque<URI>();
    stack.push(app.getFeedInitialURI());
    this.current_fragment = CatalogFeedFragment.newFragment(stack);
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

    final ArrayDeque<URI> stack = e.getStack();
    this.current_fragment = CatalogFeedFragment.newFragment(stack);

    final FragmentTransaction ft = this.fm.beginTransaction();
    ft.replace(R.id.content_frame, this.current_fragment);
    ft.addToBackStack(null);
    ft.commit();
  }
}
