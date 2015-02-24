package org.nypl.simplified.app;

import java.net.URI;

import android.app.Fragment;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The type of fragments created as part of the catalog.
 */

@SuppressWarnings("boxing") public abstract class CatalogFragment extends
  Fragment
{
  protected static final String          FEED_UP_STACK;
  private static final String            TAG;

  static {
    TAG = "CFrag";
    FEED_UP_STACK = "org.nypl.simplified.app.CatalogFragment.up_stack";
  }

  protected @Nullable ImmutableList<URI> up_stack;

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
}
