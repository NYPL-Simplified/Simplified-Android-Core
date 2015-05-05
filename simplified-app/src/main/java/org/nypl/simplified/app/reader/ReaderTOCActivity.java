package org.nypl.simplified.app.reader;

import java.util.List;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;
import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.slf4j.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
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

/**
 * Activity for displaying the table of contents on devices with small
 * screens.
 */

public final class ReaderTOCActivity extends Activity implements
  ListAdapter,
  ReaderSettingsListenerType
{
  private static final Logger LOG;
  public static final String  TOC_ID;
  public static final String  TOC_SELECTED_ID;
  public static final int     TOC_SELECTION_REQUEST_CODE;

  static {
    LOG = LogUtilities.getLog(ReaderTOCActivity.class);
    TOC_SELECTION_REQUEST_CODE = 23;
    TOC_ID = "org.nypl.simplified.app.reader.ReaderTOCActivity.toc";
    TOC_SELECTED_ID =
      "org.nypl.simplified.app.reader.ReaderTOCActivity.toc_selected";
  }

  public static void startActivityForResult(
    final Activity from,
    final ReaderTOC toc)
  {
    NullCheck.notNull(from);
    NullCheck.notNull(toc);

    final Intent i = new Intent(Intent.ACTION_PICK);
    i.setClass(from, ReaderTOCActivity.class);
    i.putExtra(ReaderTOCActivity.TOC_ID, toc);

    from.startActivityForResult(
      i,
      ReaderTOCActivity.TOC_SELECTION_REQUEST_CODE);
  }

  private @Nullable ArrayAdapter<TOCElement> adapter;
  private @Nullable ReaderTOC                toc;
  private @Nullable ImageView                view_back;
  private @Nullable ListView                 view_list;
  private @Nullable ViewGroup                view_root;
  private @Nullable TextView                 view_title;

  private void applyColorScheme(
    final ReaderColorScheme cs)
  {
    UIThread.checkIsUIThread();

    final Resources rr = NullCheck.notNull(this.getResources());
    final int main_color = rr.getColor(R.color.main_color);

    final ImageView in_back = NullCheck.notNull(this.view_back);
    final TextView in_title = NullCheck.notNull(this.view_title);
    final ViewGroup in_root = NullCheck.notNull(this.view_root);

    in_root.setBackgroundColor(cs.getBackgroundColor());
    in_title.setTextColor(main_color);
    in_back
      .setColorFilter(ReaderColorMatrix.getImageFilterMatrix(main_color));
  }

  @Override public boolean areAllItemsEnabled()
  {
    return NullCheck.notNull(this.adapter).areAllItemsEnabled();
  }

  @Override public void finish()
  {
    super.finish();
    this.overridePendingTransition(0, 0);
  }

  @Override public int getCount()
  {
    return NullCheck.notNull(this.adapter).getCount();
  }

  @Override public TOCElement getItem(
    final int position)
  {
    return NullCheck.notNull(this.adapter).getItem(position);
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

  @Override public View getView(
    final int position,
    final @Nullable View reuse,
    final @Nullable ViewGroup parent)
  {
    final ViewGroup item_view;
    if (reuse != null) {
      item_view = (ViewGroup) reuse;
    } else {
      final LayoutInflater inflater =
        (LayoutInflater) this
          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      item_view =
        (ViewGroup) inflater.inflate(
          R.layout.reader_toc_element,
          parent,
          false);
    }

    /**
     * Populate the text view and set the left margin based on the desired
     * indentation level.
     */

    final TextView text_view =
      NullCheck.notNull((TextView) item_view
        .findViewById(R.id.reader_toc_element_text));
    final TOCElement e = NullCheck.notNull(this.adapter).getItem(position);
    text_view.setText(e.getTitle());

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();
    final ReaderSettingsType settings = rs.getSettings();

    final RelativeLayout.LayoutParams p =
      new RelativeLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    p.setMargins((int) rs.screenDPToPixels(e.getIndent() * 16), 0, 0, 0);
    text_view.setLayoutParams(p);
    text_view.setTextColor(settings.getColorScheme().getForegroundColor());

    item_view.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        final Intent intent = new Intent();
        intent.putExtra(ReaderTOCActivity.TOC_SELECTED_ID, e);
        ReaderTOCActivity.this.setResult(Activity.RESULT_OK, intent);
        ReaderTOCActivity.this.finish();
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

  @Override public boolean isEmpty()
  {
    return NullCheck.notNull(this.adapter).isEmpty();
  }

  @Override public boolean isEnabled(
    final int position)
  {
    return NullCheck.notNull(this.adapter).isEnabled(position);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.reader_toc);

    ReaderTOCActivity.LOG.debug("onCreate");

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderSettingsType settings = rs.getSettings();
    settings.addListener(this);

    final Intent input = NullCheck.notNull(this.getIntent());
    final Bundle args = NullCheck.notNull(input.getExtras());

    final ReaderTOC in_toc =
      NullCheck.notNull((ReaderTOC) args
        .getSerializable(ReaderTOCActivity.TOC_ID));
    final ImageView in_back =
      NullCheck.notNull((ImageView) this.findViewById(R.id.reader_toc_back));
    final ListView in_list_view =
      NullCheck.notNull((ListView) this.findViewById(R.id.reader_toc_list));
    final TextView in_title =
      NullCheck.notNull((TextView) this.findViewById(R.id.reader_toc_title));
    final ViewGroup in_root =
      NullCheck.notNull((ViewGroup) in_list_view.getRootView());

    final List<TOCElement> es = in_toc.getElements();
    this.adapter = new ArrayAdapter<TOCElement>(this, 0, es);

    in_back.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        ReaderTOCActivity.this.finish();
      }
    });

    in_list_view.setAdapter(this);

    this.view_back = in_back;
    this.view_list = in_list_view;
    this.view_root = in_root;
    this.view_title = in_title;
    this.toc = in_toc;

    this.applyColorScheme(settings.getColorScheme());
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    ReaderTOCActivity.LOG.debug("onDestroy");

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderSettingsType settings = rs.getSettings();
    settings.removeListener(this);
  }

  @Override public void onReaderSettingsChanged(
    final ReaderSettingsType s)
  {
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        ReaderTOCActivity.this.applyColorScheme(s.getColorScheme());
      }
    });
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
