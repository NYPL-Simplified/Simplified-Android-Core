package org.nypl.simplified.app;

import java.util.List;
import java.util.concurrent.CancellationException;

import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * A book detail dialog fragment used on tablets or devices with large
 * screens.
 */

@SuppressWarnings("synthetic-access") public final class CatalogBookDialog extends
  DialogFragment
{
  private static final String ACQUISITION_ENTRY_ID;
  private static final String TAG;

  static {
    ACQUISITION_ENTRY_ID = "org.nypl.simplified.app.CatalogBookDialog.entry";
    TAG = "CBD";
  }

  private static void configureSummaryPublisher(
    final OPDSAcquisitionFeedEntry e,
    final TextView summary_publisher)
  {
    final OptionType<String> pub = e.getPublisher();
    if (pub.isSome()) {
      final Some<String> some = (Some<String>) pub;
      summary_publisher.setText(some.get());
    }
  }

  private static void configureSummaryWebView(
    final OPDSAcquisitionFeedEntry e,
    final WebView summary_text)
  {
    final StringBuilder text = new StringBuilder();
    text.append("<html>");
    text.append("<head>");
    text.append("<style>body {");
    text.append("padding: 0;");
    text.append("padding-right: 2em;");
    text.append("margin: 0;");
    text.append("}</style>");
    text.append("</head>");
    text.append("<body>");
    text.append(e.getSummary());
    text.append("</body>");
    text.append("</html>");

    final WebSettings summary_text_settings = summary_text.getSettings();
    summary_text_settings.setAllowContentAccess(false);
    summary_text_settings.setAllowFileAccess(false);
    summary_text_settings.setAllowFileAccessFromFileURLs(false);
    summary_text_settings.setAllowUniversalAccessFromFileURLs(false);
    summary_text_settings.setBlockNetworkLoads(true);
    summary_text_settings.setBlockNetworkImage(true);
    summary_text_settings.setDefaultTextEncodingName("UTF-8");
    summary_text.loadDataWithBaseURL(
      null,
      text.toString(),
      "text/html",
      "UTF-8",
      null);
  }

  private static void configureViewTextAuthor(
    final OPDSAcquisitionFeedEntry e,
    final TextView authors)
  {
    final StringBuilder buffer = new StringBuilder();
    final List<String> as = e.getAuthors();
    for (int index = 0; index < as.size(); ++index) {
      final String a = NullCheck.notNull(as.get(index));
      if (index > 0) {
        buffer.append("\n");
      }
      buffer.append(a);
    }
    authors.setText(NullCheck.notNull(buffer.toString()));
  }

  private static void configureViewTextMeta(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final TextView meta)
  {
    final StringBuilder buffer = new StringBuilder();
    CatalogBookDialog.createViewTextPublicationDate(rr, e, buffer);
    CatalogBookDialog.createViewTextPublisher(rr, e, buffer);
    CatalogBookDialog.createViewTextCategories(rr, e, buffer);
    meta.setText(NullCheck.notNull(buffer.toString()));
  }

  private static void createViewTextCategories(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final StringBuilder buffer)
  {
    final List<String> cats = e.getCategories();
    if (cats.isEmpty() == false) {
      if (buffer.length() > 0) {
        buffer.append("\n");
      }

      buffer.append(NullCheck.notNull(rr
        .getString(R.string.catalog_categories)));
      buffer.append(": ");

      for (int index = 0; index < cats.size(); ++index) {
        final String c = NullCheck.notNull(cats.get(index));
        buffer.append(c);
        if ((index + 1) <= cats.size()) {
          buffer.append(", ");
        }
      }
    }
  }

  private static String createViewTextPublicationDate(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final StringBuilder buffer)
  {
    if (buffer.length() > 0) {
      buffer.append("\n");
    }

    buffer.append(NullCheck.notNull(rr
      .getString(R.string.catalog_publication_date)));
    buffer.append(": ");
    buffer.append(e.getPublished());
    return NullCheck.notNull(buffer.toString());
  }

  private static void createViewTextPublisher(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final StringBuilder buffer)
  {
    final OptionType<String> pub = e.getPublisher();
    if (pub.isSome()) {
      final Some<String> some = (Some<String>) pub;

      if (buffer.length() > 0) {
        buffer.append("\n");
      }

      buffer.append(NullCheck.notNull(rr
        .getString(R.string.catalog_publisher)));
      buffer.append(": ");
      buffer.append(some.get());
    }
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
  private @Nullable ListenableFuture<Bitmap> loading_cover;

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

    CatalogBookDialog.configureSummaryPublisher(e, summary_publisher);
    CatalogBookDialog.configureSummaryWebView(e, summary_text);
    CatalogBookDialog.configureAcquisitions(
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

    CatalogBookDialog.configureViewTextAuthor(e, header_authors);
    CatalogBookDialog.configureViewTextMeta(rr, e, header_meta);

    related_layout.setVisibility(View.GONE);

    final Simplified app = Simplified.get();
    final CatalogAcquisitionCoverCacheType cover_loader =
      app.getCatalogAcquisitionCoverLoader();
    final int cover_height = header_cover.getLayoutParams().height;

    this.loading_cover =
      cover_loader.getCoverAsynchronous(
        e,
        new BitmapDisplayHeightPreserveAspect(cover_height),
        new BitmapCacheListenerType() {
          @Override public void onFailure(
            final Throwable x)
          {
            if (x instanceof CancellationException) {
              return;
            }

            Log.e(CatalogBookDialog.TAG, x.getMessage(), x);
          }

          @Override public void onSuccess(
            final Bitmap b)
          {
            Log.d(
              CatalogBookDialog.TAG,
              String.format(
                "returned image is (%d x %d)",
                b.getWidth(),
                b.getHeight()));

            UIThread.runOnUIThread(new Runnable() {
              @Override public void run()
              {
                header_cover.setImageBitmap(b);
              }
            });
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

    final int width = (int) (m.widthPixels * 0.75);
    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(width, window.getAttributes().height);
  }

  private static void configureAcquisitions(
    final Context ctx,
    final OPDSAcquisitionFeedEntry e,
    final ViewGroup acquisitions)
  {
    final List<OPDSAcquisition> aqs = e.getAcquisitions();
    if (aqs.isEmpty() == false) {
      for (int index = 0; index < aqs.size(); ++index) {
        final OPDSAcquisition a = NullCheck.notNull(aqs.get(index));
        final CatalogAcquisitionButton b =
          new CatalogAcquisitionButton(ctx, a);
        acquisitions.addView(b);
      }
    } else {
      acquisitions.setVisibility(View.GONE);
    }
  }

  @Override public void onDestroyView()
  {
    super.onDestroyView();

    final ListenableFuture<Bitmap> f = this.loading_cover;
    if (f != null) {
      f.cancel(true);
      this.loading_cover = null;
    }
  }
}
