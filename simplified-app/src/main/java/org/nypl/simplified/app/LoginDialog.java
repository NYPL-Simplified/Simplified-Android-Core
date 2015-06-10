package org.nypl.simplified.app;

import java.util.concurrent.atomic.AtomicBoolean;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.BooksType;
import org.slf4j.Logger;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class LoginDialog extends DialogFragment implements
  AccountLoginListenerType
{
  private static final String BARCODE_ID;
  private static final Logger LOG;
  private static final String PIN_ID;
  private static final String TEXT_ID;

  static {
    LOG = LogUtilities.getLog(DialogFragment.class);
  }

  static {
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
  private @Nullable ViewGroup                   root_layout;
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

    LogUtilities.errorWithOptionalException(LoginDialog.LOG, s, error);

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
        LoginDialog.LOG.debug("{}", e.getMessage(), e);
      }
    }
  }

  @Override public void onAccountLoginSuccess(
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    LoginDialog.LOG.debug("login succeeded");

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
        LoginDialog.LOG.debug("{}", e.getMessage(), e);
      }
    }
  }

  @Override public void onCancel(
    final @Nullable DialogInterface dialog)
  {
    LoginDialog.LOG.debug("login aborted");

    final LoginControllerListenerType ls = this.listener;
    if (ls != null) {
      try {
        ls.onLoginAborted();
      } catch (final Throwable e) {
        LoginDialog.LOG.debug("{}", e.getMessage(), e);
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
    final @Nullable LayoutInflater inflater_mn,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);
    final Bundle b = this.getArguments();
    final AccountPIN initial_pin =
      NullCheck.notNull((AccountPIN) b.getSerializable(LoginDialog.PIN_ID));
    final AccountBarcode initial_bar =
      NullCheck.notNull((AccountBarcode) b
        .getSerializable(LoginDialog.BARCODE_ID));
    final String initial_txt =
      NullCheck.notNull(b.getString(LoginDialog.TEXT_ID));

    final ViewGroup in_layout =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.login_dialog,
        container,
        false));
    this.root_layout = in_layout;

    final TextView in_text =
      NullCheck.notNull((TextView) in_layout
        .findViewById(R.id.login_dialog_text));
    final EditText in_barcode_edit =
      NullCheck.notNull((EditText) in_layout
        .findViewById(R.id.login_dialog_barcode_text_edit));
    final EditText in_pin_edit =
      NullCheck.notNull((EditText) in_layout
        .findViewById(R.id.login_dialog_pin_text_edit));
    final Button in_login_button =
      NullCheck
        .notNull((Button) in_layout.findViewById(R.id.login_dialog_ok));
    final Button in_login_cancel_button =
      NullCheck.notNull((Button) in_layout
        .findViewById(R.id.login_dialog_cancel));
    final ProgressBar in_login_progress =
      NullCheck.notNull((ProgressBar) in_layout
        .findViewById(R.id.login_progress));

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
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

        final Editable barcode_edit_text = in_barcode_edit.getText();
        final Editable pin_edit_text = in_pin_edit.getText();

        final AccountBarcode barcode =
          new AccountBarcode(NullCheck.notNull(barcode_edit_text.toString()));
        final AccountPIN pin =
          new AccountPIN(NullCheck.notNull(pin_edit_text.toString()));
        books.accountLogin(barcode, pin, LoginDialog.this);
      }
    });

    in_login_cancel_button.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        LoginDialog.this.dismiss();
      }
    });

    final AtomicBoolean in_barcode_empty = new AtomicBoolean(true);
    final AtomicBoolean in_pin_empty = new AtomicBoolean(true);

    in_barcode_edit.addTextChangedListener(new TextWatcher() {
      @Override public void afterTextChanged(
        final @Nullable Editable s)
      {
        // Nothing
      }

      @Override public void beforeTextChanged(
        final @Nullable CharSequence s,
        final int start,
        final int count,
        final int after)
      {
        // Nothing
      }

      @Override public void onTextChanged(
        final @Nullable CharSequence s,
        final int start,
        final int before,
        final int count)
      {
        in_barcode_empty.set(NullCheck.notNull(s).length() == 0);
        in_login_button.setEnabled((in_barcode_empty.get() == false)
          && (in_pin_empty.get() == false));
      }
    });

    in_pin_edit.addTextChangedListener(new TextWatcher() {
      @Override public void afterTextChanged(
        final @Nullable Editable s)
      {
        // Nothing
      }

      @Override public void beforeTextChanged(
        final @Nullable CharSequence s,
        final int start,
        final int count,
        final int after)
      {
        // Nothing
      }

      @Override public void onTextChanged(
        final @Nullable CharSequence s,
        final int start,
        final int before,
        final int count)
      {
        in_pin_empty.set(NullCheck.notNull(s).length() == 0);
        in_login_button.setEnabled((in_barcode_empty.get() == false)
          && (in_pin_empty.get() == false));
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

    return in_layout;
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

    final ViewGroup layout = NullCheck.notNull(this.root_layout);
    layout.requestLayout();
  }

  public void setLoginListener(
    final LoginControllerListenerType in_listener)
  {
    this.listener = NullCheck.notNull(in_listener);
  }
}
