package org.nypl.simplified.app;

import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * A book detail fragment used on phones or devices with small screens.
 */

public final class CatalogBookDetailFragment extends CatalogFragment
{
  private static final String FEED_ENTRY_ID;

  static {
    FEED_ENTRY_ID = "org.nypl.simplified.app.CatalogBookDetailFragment.entry";
  }

  public static CatalogBookDetailFragment newInstance(
    final OPDSAcquisitionFeedEntry e,
    final ImmutableList<URI> up_stack)
  {
    final Bundle b = new Bundle();

    b.putSerializable(
      CatalogBookDetailFragment.FEED_ENTRY_ID,
      NullCheck.notNull(e));
    b.putSerializable(
      CatalogFragment.FEED_UP_STACK,
      NullCheck.notNull(up_stack));

    final CatalogBookDetailFragment f = new CatalogBookDetailFragment();
    f.setArguments(b);
    return f;
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final Bundle args = NullCheck.notNull(this.getArguments());
    final ImmutableList<URI> u =
      NullCheck.notNull((ImmutableList<URI>) args
        .getSerializable(CatalogFragment.FEED_UP_STACK));
    this.setUpStack(u);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;

    final LinearLayout layout =
      (LinearLayout) inflater.inflate(R.layout.book_detail, container, false);
    final TextView text_view =
      (TextView) layout.findViewById(R.id.book_title);

    text_view
      .setText("You are on a small device. Therefore, you're seeing a new page here.");
    return layout;
  }
}
