package org.nypl.simplified.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.MenuItem;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

abstract class NavigableFragment extends Fragment
{
  static final String NAVIGABLE_FRAGMENT_CONTAINER_ID;
  static final String NAVIGABLE_FRAGMENT_PARENT_ID;

  static {
    NAVIGABLE_FRAGMENT_CONTAINER_ID =
      "org.nypl.simplified.app.NavigableFragment.container_id";
    NAVIGABLE_FRAGMENT_PARENT_ID =
      "org.nypl.simplified.app.NavigableFragment.parent_id";
  }

  private static Bundle newArguments(
    final int container,
    final int pid)
  {
    final Bundle args = new Bundle();
    args.putInt(NavigableFragment.NAVIGABLE_FRAGMENT_CONTAINER_ID, container);
    args.putInt(NavigableFragment.NAVIGABLE_FRAGMENT_PARENT_ID, pid);
    return args;
  }

  public static
    <F extends NavigableFragment, G extends NavigableFragment>
    void
    setFragmentArguments(
      final F f,
      final @Nullable G parent,
      final int container)
  {
    final int pid = parent != null ? parent.getNavigableID() : 0;
    final Bundle args = NavigableFragment.newArguments(container, pid);
    f.setArguments(args);
  }

  private int container_id;
  private int id;

  public int getNavigableContainerID()
  {
    return this.container_id;
  }

  public int getNavigableID()
  {
    return this.id;
  }

  public boolean isNavigableRoot()
  {
    return this.id == 1;
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final Bundle a = NullCheck.notNull(this.getArguments());
    this.container_id =
      a.getInt(NavigableFragment.NAVIGABLE_FRAGMENT_CONTAINER_ID);
    final int pid = a.getInt(NavigableFragment.NAVIGABLE_FRAGMENT_PARENT_ID);
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
}
