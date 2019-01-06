package org.nypl.simplified.app.profiles;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.R;

/**
 * A dialog telling the user that they will soon be logged out.
 */

public final class ProfileTimeOutDialog extends DialogFragment {

  private DialogInterface.OnDismissListener on_dismiss;

  /**
   * Construct a dialog.
   */

  public ProfileTimeOutDialog() {
    // Fragments must have no-arg constructors.
  }

  /**
   * @return A new dialog
   */

  public static ProfileTimeOutDialog newDialog(final DialogInterface.OnDismissListener listener) {
    final ProfileTimeOutDialog dialog = new ProfileTimeOutDialog();
    dialog.on_dismiss = NullCheck.notNull(listener, "Listener");
    return dialog;
  }

  @Override
  public void onCreate(
      final @Nullable Bundle state) {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedProfileDialog);
  }

  @Override
  public void onCancel(final DialogInterface dialog) {
    super.onCancel(dialog);
    this.on_dismiss.onDismiss(dialog);
  }

  @Override
  public void onDismiss(final DialogInterface dialog) {
    super.onDismiss(dialog);
    this.on_dismiss.onDismiss(dialog);
  }

  @Override
  public View onCreateView(
      final @Nullable LayoutInflater inflater_mn,
      final @Nullable ViewGroup container,
      final @Nullable Bundle state) {

    final LayoutInflater inflater =
        NullCheck.notNull(inflater_mn);
    final ViewGroup layout =
        NullCheck.notNull((ViewGroup) inflater.inflate(
            R.layout.profile_time_out, container, false));

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }
    return layout;
  }

  @Override
  public void onResume() {
    super.onResume();

    final Resources rr = NullCheck.notNull(this.getResources());
    final int h = (int) rr.getDimension(R.dimen.profile_time_out_dialog_height);
    final int w = (int) rr.getDimension(R.dimen.profile_time_out_dialog_width);

    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(w, h);
    window.setGravity(Gravity.CENTER);
  }

  public void setListener(final DialogInterface.OnDismissListener listener) {
    this.on_dismiss = NullCheck.notNull(listener, "Listener");
  }
}
