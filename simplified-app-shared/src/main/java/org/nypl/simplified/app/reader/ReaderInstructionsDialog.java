package org.nypl.simplified.app.reader;

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
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

/**
 * Instructions dialog for the first use of the reader.
 */

public class ReaderInstructionsDialog
  extends DialogFragment
{  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(DialogFragment.class);
  }

  private @Nullable
  Runnable on_confirm;

  /**
   * Construct a new dialog.
   */

  public ReaderInstructionsDialog()
  {
    // Fragments must have no-arg constructors.
  }

  /**
   * @return A new dialog
   */

  public static ReaderInstructionsDialog newDialog()
  {
    return new ReaderInstructionsDialog();
  }

  @Override public void onResume()
  {
    super.onResume();

    final Resources rr = NullCheck.notNull(this.getResources());
    final int h = (int) rr.getDimension(R.dimen.reader_instructions_dialog_height);
    final int w = (int) rr.getDimension(R.dimen.reader_instructions_dialog_width);

    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(w, h);
    window.setGravity(Gravity.CENTER);
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

    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.reader_instructions_dialog, container, false));

    final Button in_logout_button =
      NullCheck.notNull((Button) layout.findViewById(R.id.reader_instructions_ok));

    in_logout_button.setOnClickListener(
      new View.OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          final Runnable r = ReaderInstructionsDialog.this.on_confirm;
          ReaderInstructionsDialog.LOG.debug("runnable: {}", r);
          if (r != null) {
            r.run();
          }
          ReaderInstructionsDialog.this.dismiss();
        }
      });

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }
    return layout;
  }

  /**
   * Set the confirmation listener.
   *
   * @param r The listener
   */

  public void setOnConfirmListener(
    final Runnable r)
  {
    this.on_confirm = NullCheck.notNull(r);
  }

}
