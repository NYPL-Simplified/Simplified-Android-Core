package org.nypl.simplified.app.catalog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A book dialog requesting confirmation of the revocation of a given hold or
 * loan.
 */

public final class CatalogBookRevokeDialog extends AppCompatDialogFragment {

  private static final Logger LOG =
    LoggerFactory.getLogger(CatalogBookRevokeDialog.class);
  private static final String TYPE_KEY =
    "org.nypl.simplified.app.catalog.CatalogBookRevokeDialog.type";

  private @Nullable
  Runnable on_confirm;
  private CatalogBookRevokeType type;

  /**
   * Construct a dialog.
   */

  public CatalogBookRevokeDialog() {
    // Fragments must have no-arg constructors.
  }

  /**
   * @param type The type of revocation
   * @return A new dialog
   */

  public static CatalogBookRevokeDialog newDialog(
    final CatalogBookRevokeType type,
    final Runnable on_confirm) {
    NullCheck.notNull(type, "Revocation type");
    NullCheck.notNull(on_confirm, "On confirm");

    final CatalogBookRevokeDialog d = new CatalogBookRevokeDialog();
    final Bundle b = new Bundle();
    b.putSerializable(CatalogBookRevokeDialog.TYPE_KEY, type);
    d.setArguments(b);
    d.on_confirm = on_confirm;
    return d;
  }

  @Override
  public void onCreate(
    final @Nullable Bundle state) {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedLoginDialog);
    this.type = NullCheck.notNull(
      (CatalogBookRevokeType) this.getArguments()
        .getSerializable(CatalogBookRevokeDialog.TYPE_KEY));
  }

  @Override
  public View onCreateView(
    final @Nullable LayoutInflater inflater_mn,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state) {
    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);

    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.catalog_book_revoke_confirm, container, false));

    final Button in_revoke_confirm_button =
      NullCheck.notNull(layout.findViewById(R.id.book_revoke_confirm));

    in_revoke_confirm_button.setOnClickListener(view -> {
      final Runnable r = this.on_confirm;
      LOG.debug("runnable: {}", r);
      if (r != null) {
        r.run();
      }
      this.dismiss();
    });

    final TextView message =
      NullCheck.notNull(layout.findViewById(R.id.book_revoke_text));
    final Button in_revoke_cancel_button =
      NullCheck.notNull(layout.findViewById(R.id.book_revoke_cancel));

    final Resources rr = NullCheck.notNull(this.getResources());

    switch (this.type) {
      case REVOKE_LOAN: {
        message.setText(R.string.catalog_book_revoke_loan_confirm);
        in_revoke_confirm_button.setText(R.string.catalog_book_revoke_loan);
        in_revoke_cancel_button.setContentDescription(
          NullCheck.notNull(rr.getString(R.string.catalog_accessibility_book_revoke_loan_cancel)));
        break;
      }
      case REVOKE_HOLD: {
        message.setText(R.string.catalog_book_revoke_hold_confirm);
        in_revoke_confirm_button.setText(R.string.catalog_book_revoke_hold);
        in_revoke_cancel_button.setContentDescription(
          NullCheck.notNull(rr.getString(R.string.catalog_accessibility_book_revoke_hold_cancel)));
        break;
      }
    }

    in_revoke_cancel_button.setOnClickListener(view -> this.dismiss());

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
    final int h = (int) rr.getDimension(R.dimen.book_revoke_dialog_height);
    final int w = (int) rr.getDimension(R.dimen.book_revoke_dialog_width);

    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(w, h);
    window.setGravity(Gravity.CENTER);
  }
}
