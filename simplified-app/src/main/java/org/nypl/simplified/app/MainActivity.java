package org.nypl.simplified.app;

import java.net.URI;
import java.util.ArrayDeque;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class MainActivity extends Activity implements MainActivityType
{
  private static final String                      TAG;

  static {
    TAG = "Main";
  }

  private @Nullable ArrayDeque<URI>                catalog_upstack;
  private @Nullable ArrayDeque<SimplifiedFragment> fragment_backstack;
  private @Nullable SimplifiedFragment             fragment_current;
  private @Nullable Parts                          parts;

  @Override public
    OptionType<SimplifiedFragment>
    fragControllerGetContentFragmentCurrent()
  {
    return Option.of(this.fragment_current);
  }

  @Override public void fragControllerSetAndShowDialog(
    final DialogFragment f)
  {
    final FragmentManager fm = this.getFragmentManager();
    f.show(fm, "dialog");
  }

  @Override public
    void
    fragControllerSetContentFragmentWithBackOptionalReturn(
      final OptionType<SimplifiedFragment> return_to,
      final SimplifiedFragment f)
  {
    if (return_to.isSome()) {
      final Some<SimplifiedFragment> some =
        (Some<SimplifiedFragment>) return_to;
      this.fragControllerSetContentFragmentWithBackReturn(some.get(), f);
    } else {
      this.fragControllerSetContentFragmentWithoutBack(f);
    }
  }

  @Override public void fragControllerSetContentFragmentWithBackReturn(
    final SimplifiedFragment return_to,
    final SimplifiedFragment f)
  {
    Log.d(MainActivity.TAG, String.format(
      "Setting content to %s with back return point %s",
      f,
      return_to));

    final SimplifiedFragment fnn = NullCheck.notNull(f);
    final SimplifiedFragment rnn = NullCheck.notNull(return_to);
    final ArrayDeque<SimplifiedFragment> fbs =
      NullCheck.notNull(this.fragment_backstack);

    final FragmentManager fm = this.getFragmentManager();
    final FragmentTransaction ft = fm.beginTransaction();
    final SimplifiedFragment old_current =
      NullCheck.notNull(this.fragment_current);

    Log.d(MainActivity.TAG, String.format("Removing %s", old_current));
    ft.remove(old_current);

    Log.d(MainActivity.TAG, String.format("Adding %s", fnn));
    ft.add(R.id.content_frame, fnn);
    ft.commit();

    this.fragment_current = fnn;
    fbs.push(rnn);
  }

  @Override public void fragControllerSetContentFragmentWithoutBack(
    final SimplifiedFragment f)
  {
    Log.d(
      MainActivity.TAG,
      String.format("Setting content to %s without back return point", f));

    final SimplifiedFragment fnn = NullCheck.notNull(f);
    final FragmentManager fm = this.getFragmentManager();
    final FragmentTransaction ft = fm.beginTransaction();
    final SimplifiedFragment old_current = this.fragment_current;

    if (old_current != null) {
      Log.d(MainActivity.TAG, String.format("Removing %s", old_current));
      ft.remove(old_current);
    }

    Log.d(MainActivity.TAG, String.format("Adding %s", fnn));
    ft.add(R.id.content_frame, fnn);
    ft.commit();
    this.fragment_current = fnn;
  }

  @Override public boolean hasLargeScreen()
  {
    final Resources rr = NullCheck.notNull(this.getResources());
    final Configuration c = NullCheck.notNull(rr.getConfiguration());
    final int s = c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
    boolean large = false;
    large |=
      (s & Configuration.SCREENLAYOUT_SIZE_LARGE) == Configuration.SCREENLAYOUT_SIZE_LARGE;
    large |=
      (s & Configuration.SCREENLAYOUT_SIZE_XLARGE) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    return large;
  }

  @Override public void onBackPressed()
  {
    Log.d(MainActivity.TAG, "onBackPressed: " + this);

    final FragmentManager fm = this.getFragmentManager();
    final ArrayDeque<SimplifiedFragment> fbs =
      NullCheck.notNull(this.fragment_backstack);

    if (fbs.size() > 0) {
      final SimplifiedFragment fc = NullCheck.notNull(this.fragment_current);
      final SimplifiedFragment f_return = NullCheck.notNull(fbs.pop());
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

    this.fragment_backstack = new ArrayDeque<SimplifiedFragment>();
    this.parts = new Parts(this);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    Log.d(MainActivity.TAG, "onDestroy: " + this);
  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item)
  {
    assert item != null;
    switch (item.getItemId()) {
      case android.R.id.home:
      {
        Log.d(
          MainActivity.TAG,
          "home pressed, dispatching to current fragment");

        final OptionType<SimplifiedFragment> opt =
          this.fragControllerGetContentFragmentCurrent();
        if (opt.isSome()) {
          final Some<SimplifiedFragment> some =
            (Some<SimplifiedFragment>) opt;
          final SimplifiedFragment f = some.get();
          f.onUpButtonPressed();
        }
        return true;
      }
      default:
      {
        return super.onOptionsItemSelected(item);
      }
    }
  }
}
