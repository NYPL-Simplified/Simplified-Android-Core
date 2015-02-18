package org.nypl.simplified.app;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.io7m.jnull.Nullable;

public final class PartBooks extends Fragment implements PartType
{
  public static PartBooks newInstance()
  {
    return new PartBooks();
  }

  @Override public Fragment getCurrentFragment()
  {
    return this;
  }

  @Override public <A, E extends Exception> A matchPart(
    final PartMatcherType<A, E> m)
    throws E
  {
    return m.books(this);
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d("PartBooks", "onCreate: " + this);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;
    return inflater.inflate(R.layout.books, container, false);
  }

  @Override public void onDestroy()
  {
    super.onDestroy();
    Log.d("PartBooks", "onDestroy: " + this);
  }

  @Override public void onResume()
  {
    super.onResume();
    Log.d("PartBooks", "onResume: " + this);
  }
}
