package org.nypl.simplified.app;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.io7m.jnull.Nullable;

public final class CatalogBookFragment extends DialogFragment
{
  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedBookDialog);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;

    final LinearLayout layout =
      (LinearLayout) inflater.inflate(R.layout.book_dialog, container, false);
    final TextView text_view =
      (TextView) layout.findViewById(R.id.book_title);

    return layout;
  }
}
