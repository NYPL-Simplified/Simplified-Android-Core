package org.nypl.simplified.app.reader;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

/**
 * The table-of-contents dialog.
 */

@SuppressWarnings("synthetic-access") public final class ReaderTOCDialog
  extends DialogFragment
{
  private static final Logger LOG;
  private static final String TOC_ID;

  static {
    LOG = LogUtilities.getLog(ReaderTOCDialog.class);
    TOC_ID = "org.nypl.simplified.app.reader.ReaderTOCDialog.toc";
  }

  private @Nullable ReaderTOCSelectionListenerType receiver;
  private @Nullable ReaderTOCView                  view;

  /**
   * Construct a dialog.
   */

  public ReaderTOCDialog()
  {
    // Fragments must have no-arg constructors.
  }

  /**
   * Construct a new dialog.
   *
   * @param in_toc      The table of contents
   * @param in_receiver The listener that will item selections
   *
   * @return A new dialog
   */

  public static ReaderTOCDialog newDialog(
    final ReaderTOC in_toc,
    final ReaderTOCSelectionListenerType in_receiver)
  {
    final ReaderTOCDialog d = new ReaderTOCDialog();
    final Bundle b = new Bundle();
    b.putSerializable(ReaderTOCDialog.TOC_ID, NullCheck.notNull(in_toc));
    d.setArguments(b);
    d.setTOCReceiver(in_receiver);
    return d;
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    ReaderTOCDialog.LOG.debug("onCreate");

    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedLoginDialog);

    final Bundle b = NullCheck.notNull(this.getArguments());
    final ReaderTOC in_toc =
      NullCheck.notNull((ReaderTOC) b.getSerializable(ReaderTOCDialog.TOC_ID));

    final Activity act = NullCheck.notNull(this.getActivity());
    final LayoutInflater in_inflater =
      NullCheck.notNull(act.getLayoutInflater());

    this.view = new ReaderTOCView(
      in_inflater, act, in_toc, new ReaderTOCViewSelectionListenerType()
    {
      @Override public void onTOCBackSelected()
      {
        ReaderTOCDialog.this.dismiss();
      }

      @Override public void onTOCItemSelected(
        final TOCElement e)
      {
        NullCheck.notNull(ReaderTOCDialog.this.receiver)
          .onTOCSelectionReceived(e);
        ReaderTOCDialog.this.dismiss();
      }
    });
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    final ReaderTOCView t_view = NullCheck.notNull(this.view);
    final ViewGroup layout_view = t_view.getLayoutView();

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }

    t_view.hideTOCBackButton();
    return layout_view;
  }

  @Override public void onDestroy()
  {
    super.onDestroy();
    NullCheck.notNull(this.view).onTOCViewDestroy();
  }

  @Override public void onResume()
  {
    super.onResume();

    /**
     * Force the dialog to always appear at the same size, with a decent
     * amount of empty space around it.
     */

    final Activity act = NullCheck.notNull(this.getActivity());
    final WindowManager window_manager = NullCheck.notNull(
      (WindowManager) act.getSystemService(Context.WINDOW_SERVICE));
    final Display display =
      NullCheck.notNull(window_manager.getDefaultDisplay());

    final DisplayMetrics m = new DisplayMetrics();
    display.getMetrics(m);

    final int width = (int) (m.widthPixels * 0.80);
    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(width, window.getAttributes().height);
  }

  private void setTOCReceiver(
    final ReaderTOCSelectionListenerType in_receiver)
  {
    this.receiver = NullCheck.notNull(in_receiver);
  }
}
