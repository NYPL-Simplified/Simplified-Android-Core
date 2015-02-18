package org.nypl.simplified.app;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.io7m.jnull.Nullable;

public final class PartSettings extends Fragment implements PartType
{
  public static PartSettings newInstance()
  {
    return new PartSettings();
  }

  @Override public Fragment getCurrentFragment()
  {
    return this;
  }

  @Override public <A, E extends Exception> A matchPart(
    final PartMatcherType<A, E> m)
    throws E
  {
    return m.settings(this);
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d("PartSettings", "onCreate: " + this);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;
    return inflater.inflate(R.layout.settings, container, false);
  }

  @Override public void onDestroy()
  {
    super.onDestroy();
    Log.d("PartSettings", "onDestroy: " + this);
  }

  @Override public void onResume()
  {
    super.onResume();
    Log.d("PartSettings", "onResume: " + this);
  }
}
