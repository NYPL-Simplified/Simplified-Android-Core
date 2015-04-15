package org.nypl.simplified.app;

import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountLogoutListenerType;
import org.nypl.simplified.books.core.BooksType;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class SettingsActivity extends SimplifiedActivity implements
  AccountLogoutListenerType,
  LoginControllerListenerType
{
  private static final String TAG;

  static {
    TAG = "SA";
  }

  private static void setLoggedInText(
    final TextView t_logged,
    final boolean logged)
  {

    t_logged.setText(logged ? "Logged in" : "Not logged in");
  }

  private @Nullable TextView logged;

  @Override public void onAccountLogoutFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    Log.d(SettingsActivity.TAG, "onAccountLogoutFailure");

    if (error.isSome()) {
      final Some<Throwable> some = (Some<Throwable>) error;
      Log.e(SettingsActivity.TAG, message, some.get());
    } else {
      Log.e(SettingsActivity.TAG, message);
    }

    final SimplifiedCatalogAppServicesType app = Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    final TextView t_logged = NullCheck.notNull(this.logged);

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        final Context context = SettingsActivity.this.getApplicationContext();
        final CharSequence text = "Failed to log out: " + message;
        final int duration = Toast.LENGTH_SHORT;
        final Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        SettingsActivity.setLoggedInText(t_logged, false);
      }
    });
  }

  @Override public void onAccountLogoutSuccess()
  {
    Log.d(SettingsActivity.TAG, "onAccountLogoutSuccess");

    final SimplifiedCatalogAppServicesType app = Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    final TextView t_logged = NullCheck.notNull(this.logged);

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        final Context context = SettingsActivity.this.getApplicationContext();
        final CharSequence text = "Logged out";
        final int duration = Toast.LENGTH_SHORT;
        final Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        SettingsActivity.setLoggedInText(t_logged, false);
      }
    });
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final LayoutInflater inflater =
      NullCheck.notNull(this.getLayoutInflater());

    final FrameLayout content_area = this.getContentFrame();
    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.settings,
        content_area,
        false));
    content_area.addView(layout);
    content_area.requestLayout();

    final SimplifiedCatalogAppServicesType app = Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final TextView t_login =
      (TextView) layout.findViewById(R.id.settings_login);
    t_login.setText("Log in");
    t_login.setOnClickListener(new LoginController(this, books, this));

    final TextView t_logout =
      (TextView) layout.findViewById(R.id.settings_logout);
    t_logout.setText("Log out");
    t_logout.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        Log.d(SettingsActivity.TAG, "Logging out");
        books.accountLogout(SettingsActivity.this);
      }
    });

    final TextView t_logged =
      (TextView) layout.findViewById(R.id.settings_logged);

    SettingsActivity.setLoggedInText(t_logged, books.accountIsLoggedIn());
    this.logged = t_logged;
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
  }

  @Override public void onLoginAborted()
  {
    final SimplifiedCatalogAppServicesType app = Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    final TextView t_logged = NullCheck.notNull(this.logged);

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        SettingsActivity.setLoggedInText(t_logged, books.accountIsLoggedIn());
      }
    });
  }

  @Override public void onLoginFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    final TextView t_logged = NullCheck.notNull(this.logged);

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        SettingsActivity.setLoggedInText(t_logged, false);
      }
    });
  }

  @Override public void onLoginSuccess()
  {
    final TextView t_logged = NullCheck.notNull(this.logged);

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        SettingsActivity.setLoggedInText(t_logged, true);
      }
    });
  }

  @Override protected void onResume()
  {
    super.onResume();
  }
}
