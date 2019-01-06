package org.nypl.simplified.app.profiles;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.R;

/**
 * A dialog requesting confirmation of the user's intent to switch profiles.
 */

public final class ProfileSwitchDialog extends DialogFragment {

  private Runnable on_confirm;

  /**
   * Construct a dialog.
   */

  public ProfileSwitchDialog() {
    // Fragments must have no-arg constructors.
  }

  /**
   * @param on_confirm A runnable that will be executed on confirmation
   * @return A new dialog
   */

  public static ProfileSwitchDialog newDialog(final Runnable on_confirm) {
    final ProfileSwitchDialog dialog = new ProfileSwitchDialog();
    dialog.on_confirm = NullCheck.notNull(on_confirm);
    return dialog;
  }

  @Override
  public void onCreate(
      final @Nullable Bundle state) {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedProfileDialog);
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
            R.layout.profile_switch_confirm, container, false));
    final Button confirm_button =
        NullCheck.notNull(layout.findViewById(R.id.profile_switch_confirm));

    confirm_button.setOnClickListener(view -> {
      this.on_confirm.run();
      this.dismiss();
    });

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
    final int h = (int) rr.getDimension(R.dimen.profile_switch_dialog_height);
    final int w = (int) rr.getDimension(R.dimen.profile_switch_dialog_width);

    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(w, h);
    window.setGravity(Gravity.CENTER);
  }
}
