package org.nypl.simplified.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class ExampleFragment extends NavigableFragment
{
  /**
   * Construct a new instance without a parent.
   *
   * @param container
   *          The container
   * @return A new instance
   */

  public static NavigableFragment newInstance(
    final int container)
  {
    final ExampleFragment f = new ExampleFragment();
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
        R.layout.test_fragment,
        container,
        false));
    return view;
  }
}
