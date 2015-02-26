package org.nypl.simplified.app;

import java.util.ArrayDeque;
import java.util.Iterator;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class MainActivity extends Activity implements MainActivityType
{
  private static final String                      FRAGMENT_BACKSTACK_BASE_ID;
  private static final String                      FRAGMENT_BACKSTACK_SIZE_ID;
  private static final String                      FRAGMENT_CURRENT_ID;
  private static final String                      TAG;

  static {
    TAG = "Main";
    FRAGMENT_BACKSTACK_BASE_ID =
      "org.nypl.simplified.app.MainActivity.backstack_";
    FRAGMENT_BACKSTACK_SIZE_ID =
      "org.nypl.simplified.app.MainActivity.backstack_size";
    FRAGMENT_CURRENT_ID =
      "org.nypl.simplified.app.MainActivity.fragment_current";
  }

  private @Nullable ArrayDeque<SimplifiedFragment> fragment_backstack;
  private @Nullable SimplifiedFragment             fragment_current;
  private @Nullable Parts                          parts;

  @Override public void fragControllerDeserialize(
    final Bundle b)
  {
    Log.d(MainActivity.TAG, "deserializing fragment state");

    final FragmentManager fm = this.getFragmentManager();
    final SimplifiedFragment fc =
      NullCheck.notNull((SimplifiedFragment) fm.getFragment(
        b,
        MainActivity.FRAGMENT_CURRENT_ID));

    Log.d(
      MainActivity.TAG,
      String.format("deserialized fragment (current): %s", fc));

    final int count = b.getInt(MainActivity.FRAGMENT_BACKSTACK_SIZE_ID);

    Log.d(
      MainActivity.TAG,
      String.format("deserializing fragment stack (%d fragments)", count));

    final ArrayDeque<SimplifiedFragment> bs =
      new ArrayDeque<SimplifiedFragment>(count);
    final StringBuilder sb = new StringBuilder();
    for (int index = 0; index < count; ++index) {
      sb.setLength(0);
      sb.append(MainActivity.FRAGMENT_BACKSTACK_BASE_ID);
      sb.append(index);
      final String key = NullCheck.notNull(sb.toString());
      final SimplifiedFragment f =
        NullCheck.notNull((SimplifiedFragment) fm.getFragment(b, key));

      Log.d(
        MainActivity.TAG,
        String.format("deserialized fragment [%d] %s", index, f));

      bs.push(f);
    }

    this.fragment_backstack = bs;
    this.fragment_current = fc;
    this.fragControllerSetContentFragmentWithoutBack(fc);
  }

  @Override public
    OptionType<SimplifiedFragment>
    fragControllerGetContentFragmentCurrent()
  {
    return Option.of(this.fragment_current);
  }

  @Override public void fragControllerPopBackStack(
    final Runnable r)
  {
    final FragmentManager fm = this.getFragmentManager();
    final ArrayDeque<SimplifiedFragment> fbs =
      NullCheck.notNull(this.fragment_backstack);

    if (fbs.size() > 0) {
      final SimplifiedFragment fc = NullCheck.notNull(this.fragment_current);
      final SimplifiedFragment f_return = NullCheck.notNull(fbs.pop());
      final FragmentTransaction ft = fm.beginTransaction();

      /**
       * Because the fragment is being removed from the backstack, it's no
       * longer accessible by any means and is therefore removed with
       * <code>ft.remove()</code>.
       */

      Log.d(MainActivity.TAG, String.format("Removing %s", fc));
      ft.remove(fc);
      Log.d(MainActivity.TAG, String.format("Adding %s", f_return));
      ft.add(R.id.content_frame, f_return);
      ft.commit();

      this.fragment_current = f_return;
    } else {
      r.run();
    }
  }

  @Override public void fragControllerSerialize(
    final Bundle b)
  {
    Log.d(MainActivity.TAG, "serializing fragments");

    final FragmentManager fm = this.getFragmentManager();
    final SimplifiedFragment fc = NullCheck.notNull(this.fragment_current);

    Log.d(
      MainActivity.TAG,
      String.format("serializing fragment (current): %s", fc));

    final ArrayDeque<SimplifiedFragment> bs =
      NullCheck.notNull(this.fragment_backstack);

    /**
     * Save the current fragment.
     */

    fm.putFragment(b, MainActivity.FRAGMENT_CURRENT_ID, fc);

    /**
     * Serialize each fragment in the stack, keeping a count of how many there
     * were in order to reconstruct keys later.
     */

    Log.d(
      MainActivity.TAG,
      String.format("serializing fragment stack (%d fragments)", bs.size()));

    int save_count = 0;
    final StringBuilder sb = new StringBuilder();

    if (bs.isEmpty() == false) {
      final Iterator<SimplifiedFragment> iter = bs.descendingIterator();
      while (iter.hasNext()) {
        final SimplifiedFragment f = NullCheck.notNull(iter.next());
        sb.setLength(0);
        sb.append(MainActivity.FRAGMENT_BACKSTACK_BASE_ID);
        sb.append(save_count);
        final String key = NullCheck.notNull(sb.toString());

        Log.d(
          MainActivity.TAG,
          String.format("serializing fragment [%d] %s", save_count, f));
        fm.putFragment(b, key, f);
        ++save_count;
      }
    }

    /**
     * Save the number of serialized fragments.
     */

    {
      sb.setLength(0);
      sb.append(MainActivity.FRAGMENT_BACKSTACK_SIZE_ID);
      final String key = NullCheck.notNull(sb.toString());
      b.putInt(key, save_count);
    }

    Log.d(
      MainActivity.TAG,
      String.format("serialized %d (back) fragments", save_count));
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

    this.fragControllerPopBackStack(new Runnable() {
      @Override public void run()
      {
        Log.d(MainActivity.TAG, "back stack empty, finishing activity");
        MainActivity.this.finish();
      }
    });
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(MainActivity.TAG, "onCreate: " + this);
    this.setContentView(R.layout.main);

    if (state != null) {
      Log.d(MainActivity.TAG, "onCreate: recreating: deserializing state");
      this.fragControllerDeserialize(state);
    } else {
      Log.d(MainActivity.TAG, "onCreate: initial");
      this.fragment_backstack = new ArrayDeque<SimplifiedFragment>();
    }

    /**
     * Construct new program parts. If the activity is not being recreated
     * (most likely after an orientation change), then explicitly switch to
     * the catalog view. Otherwise, the fragment controller is responsible for
     * resetting the view to whatever it was before the orientation change
     * happened.
     */

    this.parts = new Parts(this);
    if (state == null) {
      this.parts.getPartCatalog().partSwitchTo();
    }
  }

  @Override public boolean onCreateOptionsMenu(
    final @Nullable Menu menu)
  {
    final MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.main, menu);
    return true;
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
      case R.id.tilt:
      {
        Log.d(MainActivity.TAG, "flipping orientation");

        final int o = this.getRequestedOrientation();
        if (o == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
          this
            .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
          this
            .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        return true;
      }

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

  @Override protected void onSaveInstanceState(
    final @Nullable Bundle state)
  {
    assert state != null;
    super.onSaveInstanceState(state);
    this.fragControllerSerialize(state);
  }
}
