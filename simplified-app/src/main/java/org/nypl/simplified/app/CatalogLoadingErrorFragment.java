package org.nypl.simplified.app;

import java.net.URI;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogLoadingErrorFragment extends NavigableFragment
{
  /**
   * Construct a new instance without a parent.
   *
   * @param feed_uri
   *          The URI of the feed
   * @param container
   *          The container
   * @return A new instance
   */

  public static NavigableFragment newInstance(
    final URI feed_uri,
    final int container)
  {
    NullCheck.notNull(feed_uri);

    final CatalogLoadingErrorFragment f = new CatalogLoadingErrorFragment();
    NavigableFragment.setFragmentArguments(f, null, container);
    return f;
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;
    final View view =
      NullCheck.notNull(inflater.inflate(
        R.layout.catalog_loading_error,
        container,
        false));
    return view;
  }
}
