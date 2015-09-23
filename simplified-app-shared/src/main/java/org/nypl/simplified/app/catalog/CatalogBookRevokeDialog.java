package org.nypl.simplified.app.catalog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.R;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

/**
 * A book dialog requesting confirmation of the revocation of a given hold or
 * loan.
 */

public final class CatalogBookRevokeDialog extends DialogFragment
{
  private static final Logger LOG;

  private static final String TYPE_KEY =
    "org.nypl.simplified.app.catalog.CatalogBookRevokeDialog.type";

  static {
    LOG = LogUtilities.getLog(DialogFragment.class);
  }

  private @Nullable Runnable   on_confirm;
  private CatalogBookRevokeType type;

  /**
   * Construct a dialog.
   */

  public CatalogBookRevokeDialog()
  {
    // Fragments must have no-arg constructors.
  }

  /**
   * @param type The type of revocation
   *
   * @return A new dialog
   */

  public static CatalogBookRevokeDialog newDialog(final CatalogBookRevokeType
    type)
  {
    NullCheck.notNull(type);

    final CatalogBookRevokeDialog d = new CatalogBookRevokeDialog();

    final Bundle b = new Bundle();
    b.putSerializable(CatalogBookRevokeDialog.TYPE_KEY, type);
    d.setArguments(b);
    return d;
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedLoginDialog);
    this.type = NullCheck.notNull(
      (CatalogBookRevokeType) this.getArguments()
        .getSerializable(CatalogBookRevokeDialog.TYPE_KEY));
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater_mn,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);

    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.catalog_book_revoke_confirm, container, false));

    final Button button = NullCheck.notNull(
      (Button) layout.findViewById(R.id.book_revoke_confirm));

    button.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          final Runnable r = CatalogBookRevokeDialog.this.on_confirm;
          CatalogBookRevokeDialog.LOG.debug("runnable: {}", r);
          if (r != null) {
            r.run();
          }
          CatalogBookRevokeDialog.this.dismiss();
        }
      });

    final TextView message =
      NullCheck.notNull((TextView) layout.findViewById(R.id.book_revoke_text));

    switch (this.type) {
      case REVOKE_LOAN: {
        message.setText(R.string.catalog_book_revoke_loan_confirm);
        button.setText(R.string.catalog_book_revoke_loan);
        break;
      }
      case REVOKE_HOLD: {
        message.setText(R.string.catalog_book_revoke_hold_confirm);
        button.setText(R.string.catalog_book_revoke_hold);
        break;
      }
    }

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }

    return layout;
  }

  @Override public void onResume()
  {
    super.onResume();

    final Resources rr = NullCheck.notNull(this.getResources());
    final int h = (int) rr.getDimension(R.dimen.book_revoke_dialog_height);
    final int w = (int) rr.getDimension(R.dimen.book_revoke_dialog_width);

    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(w, h);
    window.setGravity(Gravity.CENTER);
  }

  /**
   * Set the runnable that will be executed on confirmation.
   *
   * @param r The runnable
   */

  public void setOnConfirmListener(
    final Runnable r)
  {
    this.on_confirm = NullCheck.notNull(r);
  }

}
