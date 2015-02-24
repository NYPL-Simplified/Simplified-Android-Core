package org.nypl.simplified.app;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.io7m.jnull.Nullable;

/**
 * A book detail fragment used on phones or devices with small screens.
 */

public final class CatalogBookDetail extends Fragment
{
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

    return layout;
  }
}
