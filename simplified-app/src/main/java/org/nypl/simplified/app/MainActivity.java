package org.nypl.simplified.app;

import java.net.URI;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.BackStackEntry;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;

import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

public final class MainActivity extends Activity
{
  private static final String TAG;

  static {
    TAG = "MainActivity";
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(MainActivity.TAG, "onCreate: " + this);

    this.setContentView(R.layout.main);

    final FragmentManager fm = this.getFragmentManager();
    fm.addOnBackStackChangedListener(new OnBackStackChangedListener() {
      private int size = 0;

      @Override public void onBackStackChanged()
      {
        final int count = fm.getBackStackEntryCount();
        if (count > this.size) {
          Log.d("BACKSTACK", "pushed");
        } else if (count < this.size) {
          Log.d("BACKSTACK", "popped");
        } else {
          throw new UnreachableCodeException();
        }

        this.size = count;
        for (int index = 0; index < count; ++index) {
          final BackStackEntry e = fm.getBackStackEntryAt(index);
          Log.d("BACKSTACK", "[" + index + "] " + e.toString());
        }
      }
    });

    final Simplified app = Simplified.get();
    final URI uri = app.getFeedInitialURI();
    final CatalogLoadingFragment clf =
      CatalogLoadingFragment.newInstance(uri);

    this.replaceFragmentWithoutBackstack(clf);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    Log.d(MainActivity.TAG, "onDestroy: " + this);
  }

  public void replaceFragmentWithBackstack(
    final Fragment f_new)
  {
    Log.d(MainActivity.TAG, "Adding fragment (with backstack):" + f_new);

    final FragmentManager fm = this.getFragmentManager();
    final FragmentTransaction ft = fm.beginTransaction();
    ft.replace(R.id.content_frame, f_new);
    ft.addToBackStack(null);
    ft.commit();
  }

  public void replaceFragmentWithoutBackstack(
    final Fragment f_new)
  {
    Log.d(MainActivity.TAG, "Adding fragment (without backstack):" + f_new);

    final FragmentManager fm = this.getFragmentManager();
    final FragmentTransaction ft = fm.beginTransaction();
    ft.replace(R.id.content_frame, f_new);
    ft.commit();
  }
}
