package org.nypl.simplified.app;

import java.net.URI;
import java.util.ArrayDeque;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class MainActivity extends Activity implements
  FragmentControllerType
{
  private static final String            TAG;

  static {
    TAG = "MainActivity";
  }

  private @Nullable ArrayDeque<Fragment> fragment_backstack;
  private @Nullable Fragment             fragment_current;

  @Override public void onBackPressed()
  {
    Log.d(MainActivity.TAG, "onBackPressed: " + this);

    final FragmentManager fm = this.getFragmentManager();
    final ArrayDeque<Fragment> fbs =
      NullCheck.notNull(this.fragment_backstack);

    if (fbs.size() > 0) {
      final Fragment fc = NullCheck.notNull(this.fragment_current);
      final Fragment f_return = NullCheck.notNull(fbs.pop());
      final FragmentTransaction ft = fm.beginTransaction();

      Log.d(MainActivity.TAG, String.format("Removing %s", fc));
      ft.remove(fc);
      Log.d(MainActivity.TAG, String.format("Adding %s", f_return));
      ft.add(R.id.content_frame, f_return);
      ft.commit();

      this.fragment_current = f_return;
    } else {
      this.finish();
    }
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(MainActivity.TAG, "onCreate: " + this);

    this.setContentView(R.layout.main);
    this.fragment_backstack = new ArrayDeque<Fragment>();

    final Simplified app = Simplified.get();
    final URI uri = app.getFeedInitialURI();
    final CatalogLoadingFragment clf =
      CatalogLoadingFragment.newInstance(uri);

    this.setContentFragmentWithoutBack(clf);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    Log.d(MainActivity.TAG, "onDestroy: " + this);
  }

  @Override public void setContentFragmentWithBackReturn(
    final Fragment return_to,
    final Fragment f)
  {
    Log.d(MainActivity.TAG, String.format(
      "Setting content to %s with back return point %s",
      f,
      return_to));

    final Fragment fnn = NullCheck.notNull(f);
    final Fragment rnn = NullCheck.notNull(return_to);
    final ArrayDeque<Fragment> fbs =
      NullCheck.notNull(this.fragment_backstack);

    final FragmentManager fm = this.getFragmentManager();
    final FragmentTransaction ft = fm.beginTransaction();
    final Fragment old_current = NullCheck.notNull(this.fragment_current);

    Log.d(MainActivity.TAG, String.format("Removing %s", old_current));
    ft.remove(old_current);

    Log.d(MainActivity.TAG, String.format("Adding %s", fnn));
    ft.add(R.id.content_frame, fnn);
    ft.commit();

    this.fragment_current = fnn;
    fbs.push(rnn);
  }

  @Override public void setContentFragmentWithoutBack(
    final Fragment f)
  {
    Log.d(
      MainActivity.TAG,
      String.format("Setting content to %s without back return point", f));

    final Fragment fnn = NullCheck.notNull(f);
    final FragmentManager fm = this.getFragmentManager();
    final FragmentTransaction ft = fm.beginTransaction();
    final Fragment old_current = this.fragment_current;

    if (old_current != null) {
      Log.d(MainActivity.TAG, String.format("Removing %s", old_current));
      ft.remove(old_current);
    }

    Log.d(MainActivity.TAG, String.format("Adding %s", fnn));
    ft.add(R.id.content_frame, fnn);
    ft.commit();
    this.fragment_current = fnn;
  }

  @Override public void setAndShowDialog(
    final DialogFragment f)
  {
    final FragmentManager fm = this.getFragmentManager();
    f.show(fm, "dialog");
  }
}
