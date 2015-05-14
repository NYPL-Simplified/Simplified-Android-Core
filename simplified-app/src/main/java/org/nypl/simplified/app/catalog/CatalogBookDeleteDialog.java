package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

@SuppressWarnings("synthetic-access") public final class CatalogBookDeleteDialog extends
  DialogFragment
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(DialogFragment.class);
  }

  public static CatalogBookDeleteDialog newDialog()
  {
    final CatalogBookDeleteDialog d = new CatalogBookDeleteDialog();
    return d;
  }

  private @Nullable Runnable on_confirm;

  public CatalogBookDeleteDialog()
  {
    // Fragments must have no-arg constructors.
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

    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_book_delete_confirm,
        container,
        false));

    final Button in_delete_button =
      NullCheck.notNull((Button) layout
        .findViewById(R.id.book_delete_confirm));
    in_delete_button.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        final Runnable r = CatalogBookDeleteDialog.this.on_confirm;
        CatalogBookDeleteDialog.LOG.debug("runnable: {}", r);
        if (r != null) {
          r.run();
        }
        CatalogBookDeleteDialog.this.dismiss();
      }
    });

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

  public void setOnConfirmListener(
    final Runnable r)
  {
    this.on_confirm = NullCheck.notNull(r);
  }
}
