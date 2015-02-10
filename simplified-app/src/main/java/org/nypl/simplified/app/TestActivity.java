package org.nypl.simplified.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

public final class TestActivity extends Activity
{
  private static void displayOrHideUpCaretAsNecessary(
    final FragmentManager fm,
    final ActionBar bar)
  {
    final boolean stack_nonempty = fm.getBackStackEntryCount() > 0;
    bar.setDisplayHomeAsUpEnabled(stack_nonempty);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.test);

    final FragmentManager fm = this.getFragmentManager();

    final ActionBar bar = this.getActionBar();
    fm.addOnBackStackChangedListener(new OnBackStackChangedListener() {
      @Override public void onBackStackChanged()
      {
        TestActivity.displayOrHideUpCaretAsNecessary(fm, bar);
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

    TestActivity.displayOrHideUpCaretAsNecessary(fm, bar);
  }
}
