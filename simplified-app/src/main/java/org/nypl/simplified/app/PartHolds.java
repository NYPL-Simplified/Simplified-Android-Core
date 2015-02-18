package org.nypl.simplified.app;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.io7m.jnull.Nullable;

public final class PartHolds extends Fragment implements PartType
{
  public static PartHolds newInstance()
  {
    return new PartHolds();
  }

  @Override public Fragment getCurrentFragment()
  {
    return this;
  }

  @Override public <A, E extends Exception> A matchPart(
    final PartMatcherType<A, E> m)
    throws E
  {
    return m.holds(this);
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d("PartHolds", "onCreate: " + this);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle savedInstanceState)
  {
    assert inflater != null;
    return inflater.inflate(R.layout.holds, container, false);
  }

  @Override public void onDestroy()
  {
    super.onDestroy();
    Log.d("PartHolds", "onDestroy: " + this);
  }

  @Override public void onResume()
  {
    super.onResume();
    Log.d("PartHolds", "onResume: " + this);
  }
}
