package org.nypl.simplified.app;

import android.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class HoldsFragment extends SimplifiedFragment
{
  private static final String TAG;

  static {
    TAG = "Holds";
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;

    final LinearLayout view =
      (LinearLayout) inflater.inflate(R.layout.holds, container, false);

    return NullCheck.notNull(view);
  }

  @Override public void onUpButtonConfigure()
  {
    final MainActivity act = (MainActivity) this.getActivity();
    final ActionBar bar = act.getActionBar();
    bar.setDisplayHomeAsUpEnabled(false);
  }

  @Override public void onUpButtonPressed()
  {
    Log.d(HoldsFragment.TAG, "up button pressed");
  }
}
