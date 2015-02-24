package org.nypl.simplified.app;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class SettingsFragment extends Fragment
{
  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;

    final LinearLayout view =
      (LinearLayout) inflater.inflate(R.layout.settings, container, false);

    return NullCheck.notNull(view);
  }
}
