package org.nypl.simplified.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.BackStackEntry;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

public final class TestActivity extends Activity
{
  private static void displayOrHideUpCaretAsNecessary(
    final Resources rr,
    final FragmentManager fm,
    final ActionBar bar)
  {
    final int count = fm.getBackStackEntryCount();
    final boolean stack_nonempty = count > 0;
    if (stack_nonempty) {
      final BackStackEntry e = fm.getBackStackEntryAt(count - 1);
      bar.setTitle(String.format("You are at: %s", e.getName()));
    } else {
      bar.setTitle(rr.getString(R.string.app_name));
    }

    bar.setDisplayHomeAsUpEnabled(stack_nonempty);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.test);

    final Resources rr = this.getResources();
    final FragmentManager fm = this.getFragmentManager();
    final ActionBar bar = this.getActionBar();
    bar.setDisplayShowTitleEnabled(true);

    fm.addOnBackStackChangedListener(new OnBackStackChangedListener() {
      @Override public void onBackStackChanged()
      {
        TestActivity.displayOrHideUpCaretAsNecessary(rr, fm, bar);
      }
    });

    final Fragment f = fm.findFragmentById(R.id.frag_container);
    if (f == null) {
      final TestFragment fn =
        TestFragment.newFragment(null, R.id.frag_container);
      final FragmentTransaction ft = fm.beginTransaction();
      ft.add(R.id.frag_container, fn);
      ft.commit();
    }

    TestActivity.displayOrHideUpCaretAsNecessary(rr, fm, bar);
  }
}
