package org.nypl.simplified.app;

import java.net.URI;

import android.app.ActionBar;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The type of fragments created as part of the catalog.
 */

@SuppressWarnings("boxing") public abstract class CatalogFragment extends
  SimplifiedFragment
{
  protected static final String        FEED_UP_STACK;
  private static final String          TAG;

  static {
    TAG = "CFrag";
    FEED_UP_STACK = "org.nypl.simplified.app.CatalogFragment.up_stack";
  }

  private @Nullable ImmutableList<URI> up_stack;

  /**
   * Log the current up stack.
   */

  protected final void debugShowUpStack()
  {
    final ImmutableList<URI> us = NullCheck.notNull(this.up_stack);
    if (us.size() > 0) {
      for (int index = 0; index < us.size(); ++index) {
        final URI e = us.get(index);
        Log.d(CatalogFragment.TAG, String.format("[%d] %s", index, e));
      }
    } else {
      Log.d(CatalogFragment.TAG, "up stack is empty");
    }
  }

  /**
   * @return The current list of URIs that led to the current fragment from
   *         the root. This list will be empty for the root of the catalog.
   */

  protected final ImmutableList<URI> getUpStack()
  {
    return NullCheck.notNull(this.up_stack);
  }

  @Override public void onUpButtonConfigure()
  {
    /**
     * If the <i>up stack</i> is non-empty for this fragment, display an
     * <i>up</i> button in the action bar.
     */

    final MainActivity act = (MainActivity) this.getActivity();
    final ActionBar bar = act.getActionBar();
    final ImmutableList<URI> us = this.getUpStack();
    final boolean enabled = us.isEmpty() == false;
    Log.d(
      CatalogFragment.TAG,
      String.format("configuring up button (enabled: %s)", enabled));
    bar.setDisplayHomeAsUpEnabled(enabled);
    bar.setHomeButtonEnabled(enabled);
  }

  @Override public void onUpButtonPressed()
  {
    Log.d(CatalogFragment.TAG, "up button pressed");

    final MainActivity act = (MainActivity) this.getActivity();

    final ImmutableList<URI> us = this.getUpStack();
    assert us.isEmpty() == false;

    final URI previous = NullCheck.notNull(us.get(us.size() - 1));
    final ImmutableList<URI> new_stack =
      NullCheck.notNull(us.subList(0, us.size() - 1));

    final CatalogLoadingFragment clf =
      CatalogLoadingFragment.newInstance(previous, new_stack);
    act.fragControllerSetContentFragmentWithBackReturn(this, clf);
  }

  /**
   * Set the up stack.
   *
   * @param u
   *          The stack
   */

  protected final void setUpStack(
    final ImmutableList<URI> u)
  {
    this.up_stack = NullCheck.notNull(u);
  }
}
