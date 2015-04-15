package org.nypl.simplified.app;

import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.BooksType;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class LoginDialog extends DialogFragment implements
  AccountLoginListenerType
{
  private static final String                   BARCODE_ID;
  private static final String                   PIN_ID;
  private static final String                   TAG;
  private static final String                   TEXT_ID;

  static {
    TAG = "LD";
    BARCODE_ID = "org.nypl.simplified.app.LoginDialog.barcode";
    PIN_ID = "org.nypl.simplified.app.LoginDialog.pin";
    TEXT_ID = "org.nypl.simplified.app.LoginDialog.text";
  }

  public static LoginDialog newDialog(
    final String text,
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    NullCheck.notNull(text);
    NullCheck.notNull(barcode);
    NullCheck.notNull(pin);

    final Bundle b = new Bundle();
    b.putSerializable(LoginDialog.TEXT_ID, text);
    b.putSerializable(LoginDialog.PIN_ID, pin);
    b.putSerializable(LoginDialog.BARCODE_ID, barcode);

    final LoginDialog d = new LoginDialog();
    d.setArguments(b);
    return d;
  }
  private @Nullable EditText                    barcode_edit;
  private @Nullable LoginControllerListenerType listener;
  private @Nullable Button                      login;
  private @Nullable ProgressBar                 login_progress;
  private @Nullable EditText                    pin_edit;

  private @Nullable TextView                    text;

  public LoginDialog()
  {
    // Fragments must have no-arg constructors.
  }

  @Override public void onAccountLoginFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    final String s =
      NullCheck.notNull(String.format("login failed: %s", message));

    if (error.isSome()) {
      final Some<Throwable> some = (Some<Throwable>) error;
      Log.e(LoginDialog.TAG, s, some.get());
    } else {
      Log.e(LoginDialog.TAG, s);
    }

    final TextView in_text = NullCheck.notNull(this.text);
    final EditText in_barcode_edit = NullCheck.notNull(this.barcode_edit);
    final EditText in_pin_edit = NullCheck.notNull(this.pin_edit);
    final Button in_login = NullCheck.notNull(this.login);
    final ProgressBar in_progress = NullCheck.notNull(this.login_progress);

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        in_text.setText(message);
        in_barcode_edit.setEnabled(true);
        in_pin_edit.setEnabled(true);
        in_login.setEnabled(true);
        in_login.setVisibility(View.VISIBLE);
        in_progress.setVisibility(View.GONE);
      }
    });

    final LoginControllerListenerType ls = this.listener;
    if (ls != null) {
      try {
        ls.onLoginFailure(error, message);
      } catch (final Throwable e) {
        Log.d(LoginDialog.TAG, e.getMessage(), e);
      }
    }
  }

  @Override public void onAccountLoginSuccess(
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    Log.d(LoginDialog.TAG, "login succeeded");

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        LoginDialog.this.dismiss();
      }
    });

    final LoginControllerListenerType ls = this.listener;
    if (ls != null) {
      try {
        ls.onLoginSuccess();
      } catch (final Throwable e) {
        Log.d(LoginDialog.TAG, e.getMessage(), e);
      }
    }
  }

  @Override public void onCancel(
    final @Nullable DialogInterface dialog)
  {
    Log.d(LoginDialog.TAG, "login aborted");

    final LoginControllerListenerType ls = this.listener;
    if (ls != null) {
      try {
        ls.onLoginAborted();
      } catch (final Throwable e) {
        Log.d(LoginDialog.TAG, e.getMessage(), e);
      }
    }
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedLoginDialog);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;

    final Bundle b = this.getArguments();
    final AccountPIN initial_pin =
      NullCheck.notNull((AccountPIN) b.getSerializable(LoginDialog.PIN_ID));
    final AccountBarcode initial_bar =
      NullCheck.notNull((AccountBarcode) b
        .getSerializable(LoginDialog.BARCODE_ID));
    final String initial_txt =
      NullCheck.notNull(b.getString(LoginDialog.TEXT_ID));

    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.login_dialog,
        container,
        false));

    final TextView in_text =
      NullCheck.notNull((TextView) layout
        .findViewById(R.id.login_dialog_text));
    final EditText in_barcode_edit =
      NullCheck.notNull((EditText) layout
        .findViewById(R.id.login_dialog_barcode_text_edit));
    final EditText in_pin_edit =
      NullCheck.notNull((EditText) layout
        .findViewById(R.id.login_dialog_pin_text_edit));
    final Button in_login_button =
      NullCheck.notNull((Button) layout.findViewById(R.id.login_dialog_ok));
    final ProgressBar in_login_progress =
      NullCheck.notNull((ProgressBar) layout
        .findViewById(R.id.login_progress));

    final SimplifiedCatalogAppServicesType app = Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    in_text.setText(initial_txt);
    in_barcode_edit.setText(initial_bar.toString());
    in_pin_edit.setText(initial_pin.toString());

    in_login_progress.setVisibility(View.GONE);
    in_login_button.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View button)
      {
        in_barcode_edit.setEnabled(false);
        in_pin_edit.setEnabled(false);
        in_login_button.setEnabled(false);
        in_login_button.setVisibility(View.GONE);
        in_login_progress.setVisibility(View.VISIBLE);

        final AccountBarcode barcode =
          new AccountBarcode(in_barcode_edit.getText().toString());
        final AccountPIN pin =
          new AccountPIN(in_pin_edit.getText().toString());
        books.accountLogin(barcode, pin, LoginDialog.this);
      }
    });

    this.barcode_edit = in_barcode_edit;
    this.pin_edit = in_pin_edit;
    this.login = in_login_button;
    this.login_progress = in_login_progress;
    this.text = in_text;

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }
    return layout;
  }

  @Override public void onResume()
  {
    super.onResume();

    /**
     * Force the dialog to always appear at the same size, with a decent
     * amount of empty space around it.
     */

    final Activity act = NullCheck.notNull(this.getActivity());
    final WindowManager window_manager =
      NullCheck.notNull((WindowManager) act
        .getSystemService(Context.WINDOW_SERVICE));
    final Display display =
      NullCheck.notNull(window_manager.getDefaultDisplay());

    final DisplayMetrics m = new DisplayMetrics();
    display.getMetrics(m);

    final int width = (int) (m.widthPixels * 0.80);
    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(width, window.getAttributes().height);
  }

  public void setLoginListener(
    final LoginControllerListenerType in_listener)
  {
    this.listener = NullCheck.notNull(in_listener);
  }
}
