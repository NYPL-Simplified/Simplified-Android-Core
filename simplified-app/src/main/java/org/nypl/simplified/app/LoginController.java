package org.nypl.simplified.app;

import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountsType;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class LoginController implements
  OnClickListener,
  LoginControllerListenerType
{
  private static final String               TAG;

  static {
    TAG = "LC";
  }

  private final AccountsType                accounts;
  private final Activity                    activity;
  private final LoginControllerListenerType listener;

  public LoginController(
    final Activity in_activity,
    final AccountsType in_accounts,
    final LoginControllerListenerType in_listener)
  {
    this.activity = NullCheck.notNull(in_activity);
    this.accounts = NullCheck.notNull(in_accounts);
    this.listener = NullCheck.notNull(in_listener);
  }

  private void go()
  {
    if (this.accounts.accountIsLoggedIn()) {
      this.listener.onLoginSuccess();
    } else {
      this.tryLogin();
    }
  }

  @Override public void onClick(
    final @Nullable View v)
  {
    this.go();
  }

  @Override public void onLoginAborted()
  {
    this.listener.onLoginAborted();
  }

  @Override public void onLoginFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    Log.d(LoginController.TAG, "onLoginFailure");

    if (error.isSome()) {
      final Some<Throwable> some = (Some<Throwable>) error;
      Log.e(LoginController.TAG, message, some.get());
    } else {
      Log.e(LoginController.TAG, message);
    }

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        final Context context =
          LoginController.this.activity.getApplicationContext();
        final CharSequence text = "Failed to log in: " + message;
        final int duration = Toast.LENGTH_SHORT;
        final Toast toast = Toast.makeText(context, text, duration);
        toast.show();
      }
    });

    this.listener.onLoginFailure(error, message);
  }

  @Override public void onLoginSuccess()
  {
    Log.d(LoginController.TAG, "onLoginSuccess");

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        final Context context =
          LoginController.this.activity.getApplicationContext();
        final CharSequence text = "Logged in";
        final int duration = Toast.LENGTH_SHORT;
        final Toast toast = Toast.makeText(context, text, duration);
        toast.show();
      }
    });

    this.listener.onLoginSuccess();
  }

  private void tryLogin()
  {
    final AccountBarcode barcode = new AccountBarcode("");
    final AccountPIN pin = new AccountPIN("");

    final LoginDialog df =
      LoginDialog.newDialog("Login required", barcode, pin);
    df.setLoginListener(this);

    final FragmentManager fm = this.activity.getFragmentManager();
    df.show(fm, "login-dialog");
  }
}
