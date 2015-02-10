package org.nypl.simplified.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class TestFragment extends Fragment
{
  static final String TEST_FRAGMENT_CONTAINER_ID;
  static final String TEST_FRAGMENT_PARENT_ID;

  static {
    TEST_FRAGMENT_CONTAINER_ID =
      "org.nypl.simplified.app.TestFragment.container_id";
    TEST_FRAGMENT_PARENT_ID =
      "org.nypl.simplified.app.TestFragment.parent_id";
  }

  private int         container_id;
  private int         id;

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final String name = String.format("button-%d", this.id);
    Log.d("test-fragment", "creating " + name);

    final Bundle a = NullCheck.notNull(this.getArguments());
    this.container_id = a.getInt(TestFragment.TEST_FRAGMENT_CONTAINER_ID);
    final int pid = a.getInt(TestFragment.TEST_FRAGMENT_PARENT_ID);
    this.id = pid + 1;

    this.setHasOptionsMenu(true);
  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item)
  {
    assert item != null;
    switch (item.getItemId()) {
      case android.R.id.home:
      {
        Log.d("test-fragment", "received home button");
        final FragmentManager fm = this.getFragmentManager();
        fm.popBackStack();
        return true;
      }
      default:
      {
        return super.onOptionsItemSelected(item);
      }
    }
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    final String name = String.format("button-%d", this.id);
    Log.d("test-fragment", "view " + name);

    assert inflater != null;
    final View view =
      inflater.inflate(R.layout.test_fragment, container, false);

    final int cid = this.container_id;
    final FragmentManager fm = this.getFragmentManager();
    final Button b = (Button) view.findViewById(R.id.test_button);
    b.setText(name);
    b.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        Log.d("test-fragment", "pressed " + name);

        final FragmentTransaction ft = fm.beginTransaction();
        final TestFragment f =
          TestFragment.newFragment(TestFragment.this, cid);

        ft.replace(cid, f);
        ft.addToBackStack(name);
        ft.commit();
      }
    });

    return view;
  }

  public static TestFragment newFragment(
    final @Nullable TestFragment parent,
    final int container)
  {
    final int pid = parent != null ? parent.id : 0;

    final Bundle args = new Bundle();
    args.putInt(TestFragment.TEST_FRAGMENT_CONTAINER_ID, container);
    args.putInt(TestFragment.TEST_FRAGMENT_PARENT_ID, pid);

    final TestFragment fn = new TestFragment();
    fn.setArguments(args);
    return fn;
  }
}
