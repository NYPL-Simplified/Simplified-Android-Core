package org.nypl.simplified.app.reader;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;
import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.slf4j.Logger;

import java.util.List;

/**
 * A re-usable view of a table of contents.
 */

@SuppressWarnings("synthetic-access") public final class ReaderTOCView
  implements ListAdapter, ReaderSettingsListenerType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderTOCView.class);
  }

  private final ArrayAdapter<TOCElement>           adapter;
  private final Context                            context;
  private final LayoutInflater                     inflater;
  private final ReaderTOCViewSelectionListenerType listener;
  private final ImageView                          view_back;
  private final ViewGroup                          view_layout;
  private final ViewGroup                          view_root;
  private final TextView                           view_title;

  /**
   * Construct a TOC view.
   *
   * @param in_inflater A layout inflater
   * @param in_context  A context
   * @param in_toc      The table of contents
   * @param in_listener A selection listener
   */

  public ReaderTOCView(
    final LayoutInflater in_inflater,
    final Context in_context,
    final ReaderTOC in_toc,
    final ReaderTOCViewSelectionListenerType in_listener)
  {
    NullCheck.notNull(in_inflater);
    NullCheck.notNull(in_context);
    NullCheck.notNull(in_toc);
    NullCheck.notNull(in_listener);

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderSettingsType settings = rs.getSettings();
    settings.addListener(this);

    final ViewGroup in_layout = NullCheck.notNull(
      (ViewGroup) in_inflater.inflate(
        R.layout.reader_toc, null));

    final ImageView in_back = NullCheck.notNull(
      (ImageView) in_layout.findViewById(R.id.reader_toc_back));
    final ListView in_list_view = NullCheck.notNull(
      (ListView) in_layout.findViewById(R.id.reader_toc_list));
    final TextView in_title = NullCheck.notNull(
      (TextView) in_layout.findViewById(R.id.reader_toc_title));
    final ViewGroup in_root =
      NullCheck.notNull((ViewGroup) in_list_view.getRootView());

    final List<TOCElement> es = in_toc.getElements();
    this.adapter = new ArrayAdapter<TOCElement>(in_context, 0, es);

    in_back.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          in_listener.onTOCBackSelected();
        }
      });

    in_list_view.setAdapter(this);

    this.context = in_context;
    this.view_layout = in_layout;
    this.view_back = in_back;
    this.view_root = in_root;
    this.view_title = in_title;
    this.inflater = in_inflater;
    this.listener = in_listener;

    this.applyColorScheme(
      NullCheck.notNull(in_context.getResources()), settings.getColorScheme());
  }

  private void applyColorScheme(
    final Resources r,
    final ReaderColorScheme cs)
  {
    UIThread.checkIsUIThread();

    final int main_color = r.getColor(R.color.feature_main_color);
    final ImageView in_back = NullCheck.notNull(this.view_back);
    final TextView in_title = NullCheck.notNull(this.view_title);
    final ViewGroup in_root = NullCheck.notNull(this.view_root);

    in_root.setBackgroundColor(cs.getBackgroundColor());
    in_title.setTextColor(main_color);
    in_back.setColorFilter(ReaderColorMatrix.getImageFilterMatrix(main_color));
  }

  @Override public boolean areAllItemsEnabled()
  {
    return NullCheck.notNull(this.adapter).areAllItemsEnabled();
  }

  @Override public int getCount()
  {
    return NullCheck.notNull(this.adapter).getCount();
  }

  @Override public TOCElement getItem(
    final int position)
  {
    return NullCheck.notNull(
      NullCheck.notNull(this.adapter).getItem(position));
  }

  @Override public long getItemId(
    final int position)
  {
    return NullCheck.notNull(this.adapter).getItemId(position);
  }

  @Override public int getItemViewType(
    final int position)
  {
    return NullCheck.notNull(this.adapter).getItemViewType(position);
  }

  /**
   * @return The view group containing the main layout
   */

  public ViewGroup getLayoutView()
  {
    return this.view_layout;
  }

  @Override public View getView(
    final int position,
    final @Nullable View reuse,
    final @Nullable ViewGroup parent)
  {
    final ViewGroup item_view;
    if (reuse != null) {
      item_view = (ViewGroup) reuse;
    } else {
      item_view = (ViewGroup) this.inflater.inflate(
        R.layout.reader_toc_element, parent, false);
    }

    /**
     * Populate the text view and set the left margin based on the desired
     * indentation level.
     */

    final TextView text_view = NullCheck.notNull(
      (TextView) item_view.findViewById(R.id.reader_toc_element_text));
    final TOCElement e = NullCheck.notNull(this.adapter).getItem(position);
    text_view.setText(e.getTitle());

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();
    final ReaderSettingsType settings = rs.getSettings();

    final RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
      android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
      android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    p.setMargins((int) rs.screenDPToPixels(e.getIndent() * 16), 0, 0, 0);
    text_view.setLayoutParams(p);
    text_view.setTextColor(settings.getColorScheme().getForegroundColor());

    item_view.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          ReaderTOCView.this.listener.onTOCItemSelected(e);
        }
      });

    return item_view;
  }

  @Override public int getViewTypeCount()
  {
    return NullCheck.notNull(this.adapter).getViewTypeCount();
  }

  @Override public boolean hasStableIds()
  {
    return NullCheck.notNull(this.adapter).hasStableIds();
  }

  /**
   * Hide the back button!
   */

  public void hideTOCBackButton()
  {
    this.view_back.setEnabled(false);
    this.view_back.setVisibility(View.GONE);
  }

  @Override public boolean isEmpty()
  {
    return NullCheck.notNull(this.adapter).isEmpty();
  }

  @Override public boolean isEnabled(
    final int position)
  {
    return NullCheck.notNull(this.adapter).isEnabled(position);
  }

  @Override public void onReaderSettingsChanged(
    final ReaderSettingsType s)
  {
    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          ReaderTOCView.this.applyColorScheme(
            NullCheck.notNull(ReaderTOCView.this.context.getResources()),
            s.getColorScheme());
        }
      });
  }

  /**
   * Called when a table of contents is destroyed.
   */

  public void onTOCViewDestroy()
  {
    ReaderTOCView.LOG.debug("onTOCViewDestroy");

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderSettingsType settings = rs.getSettings();
    settings.removeListener(this);
  }

  @Override public void registerDataSetObserver(
    final @Nullable DataSetObserver observer)
  {
    NullCheck.notNull(this.adapter).registerDataSetObserver(observer);
  }

  @Override public void unregisterDataSetObserver(
    final @Nullable DataSetObserver observer)
  {
    NullCheck.notNull(this.adapter).unregisterDataSetObserver(observer);
  }
}
