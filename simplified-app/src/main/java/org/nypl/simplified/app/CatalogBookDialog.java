package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * A book detail dialog fragment used on tablets or devices with large
 * screens.
 */

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class CatalogBookDialog extends
  DialogFragment
{
  private static final String ACQUISITION_ENTRY_ID;
  private static final String TAG;

  static {
    ACQUISITION_ENTRY_ID = "org.nypl.simplified.app.CatalogBookDialog.entry";
    TAG = "CBD";
  }

  public static CatalogBookDialog newDialog(
    final OPDSAcquisitionFeedEntry e)
  {
    final CatalogBookDialog c = new CatalogBookDialog();
    final Bundle b = new Bundle();
    b.putSerializable(
      CatalogBookDialog.ACQUISITION_ENTRY_ID,
      NullCheck.notNull(e));
    c.setArguments(b);
    return c;
  }

  private @Nullable OPDSAcquisitionFeedEntry entry;

  public CatalogBookDialog()
  {
    // Fragments must have no-arg constructors.
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedBookDialog);

    final Bundle b = NullCheck.notNull(this.getArguments());
    final OPDSAcquisitionFeedEntry e =
      NullCheck.notNull((OPDSAcquisitionFeedEntry) b
        .getSerializable(CatalogBookDialog.ACQUISITION_ENTRY_ID));
    Log.d(CatalogBookDialog.TAG, "showing dialog for " + e.getID());
    this.entry = e;
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;

    final Resources rr = NullCheck.notNull(this.getResources());
    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry);

    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.book_dialog,
        container,
        false));

    final ViewGroup header =
      NullCheck.notNull((ViewGroup) layout.findViewById(R.id.book_header));
    final ViewGroup header_left =
      NullCheck.notNull((ViewGroup) header
        .findViewById(R.id.book_header_left));
    final TextView header_title =
      NullCheck.notNull((TextView) header
        .findViewById(R.id.book_header_title));
    final TextView header_subtitle =
      NullCheck.notNull((TextView) header
        .findViewById(R.id.book_header_subtitle));
    final ImageView header_cover =
      NullCheck.notNull((ImageView) header
        .findViewById(R.id.book_header_cover));
    final TextView header_authors =
      NullCheck.notNull((TextView) header
        .findViewById(R.id.book_header_authors));
    final TextView header_meta =
      NullCheck
        .notNull((TextView) header.findViewById(R.id.book_header_meta));

    final ViewGroup hold_notification =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_hold_notification));

    final ViewGroup acquisitions =
      NullCheck.notNull((ViewGroup) layout.findViewById(R.id.book_buttons));

    final ViewGroup summary =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_summary_layout));
    final TextView summary_publisher =
      NullCheck.notNull((TextView) summary
        .findViewById(R.id.book_summary_publisher));
    final WebView summary_text =
      NullCheck.notNull((WebView) summary
        .findViewById(R.id.book_summary_text));

    final ViewGroup related_layout =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_related_layout));

    /**
     * Assuming a roughly fixed height for cover images, assume a 4:3 aspect
     * ratio and set the width of the cover layout.
     */

    final int cover_height = header_cover.getLayoutParams().height;
    final int cover_width = (int) ((cover_height / 4.0) * 3.0);
    final LinearLayout.LayoutParams cp =
      new LinearLayout.LayoutParams(cover_width, LayoutParams.WRAP_CONTENT);
    header_left.setLayoutParams(cp);

    /**
     * Configure detail texts.
     */

    CatalogBookDetail.configureSummaryPublisher(e, summary_publisher);
    CatalogBookDetail.configureSummaryWebView(e, summary_text);
    CatalogBookDetail.configureAcquisitions(
      NullCheck.notNull(this.getActivity()),
      e,
      acquisitions);

    hold_notification.setVisibility(View.GONE);
    header_title.setText(e.getTitle());

    if (e.getSubtitle().isEmpty() == false) {
      header_subtitle.setText(e.getSubtitle());
    } else {
      header_subtitle.setVisibility(View.GONE);
    }

    CatalogBookDetail.configureViewTextAuthor(e, header_authors);
    CatalogBookDetail.configureViewTextMeta(rr, e, header_meta);

    related_layout.setVisibility(View.GONE);

    final Simplified app = Simplified.get();
    final CoverProviderType cover_provider = app.getCoverProvider();
    cover_provider.loadCoverInto(e, header_cover, cover_width, cover_height);

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

    final int width = (int) (m.widthPixels * 0.75);
    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(width, window.getAttributes().height);
  }
}
