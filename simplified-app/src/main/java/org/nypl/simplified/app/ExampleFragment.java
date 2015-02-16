package org.nypl.simplified.app;

import java.util.Random;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.Button;

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

  public static NavigableFragment newInstanceWithoutParent(
    final int container)
  {
    final ExampleFragment f = new ExampleFragment();
    NavigableFragment.setFragmentArguments(f, null, container);
    return f;
  }

  /**
   * Construct a new instance with a parent.
   *
   * @param container
   *          The container
   * @return A new instance
   */

  public static NavigableFragment newInstanceWithParent(
    final NavigableFragment parent,
    final int container)
  {
    final ExampleFragment f = new ExampleFragment();
    NavigableFragment.setFragmentArguments(
      f,
      NullCheck.notNull(parent),
      container);
    return f;
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
  }

  private static int COUNT = 0;

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

    final Button button = (Button) view.findViewById(R.id.test_button);
    final AbsoluteLayout.LayoutParams params =
      (AbsoluteLayout.LayoutParams) button.getLayoutParams();

    final Random r = new Random();
    params.x = r.nextInt(300);
    params.y = r.nextInt(300);
    button.setLayoutParams(params);
    button.setText("Go");
    ExampleFragment.COUNT = ExampleFragment.COUNT + 1;

    final String title = String.format("%d", ExampleFragment.COUNT);
    final FragmentManager fm = this.getFragmentManager();
    final int cid = this.getNavigableContainerID();
    button.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        final NavigableFragment fn =
          ExampleFragment.newInstanceWithParent(ExampleFragment.this, cid);
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(cid, fn);
        ft.addToBackStack(title);
        ft.commit();
      }
    });

    return view;
  }
}
