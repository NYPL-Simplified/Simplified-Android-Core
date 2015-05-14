package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;
import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * Activity for displaying the table of contents on devices with small
 * screens.
 */

public final class ReaderTOCActivity extends Activity implements
  ReaderSettingsListenerType,
  ReaderTOCViewSelectionListenerType
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

  private @Nullable ReaderTOCView view;

  @Override public void finish()
  {
    super.finish();
    this.overridePendingTransition(0, 0);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

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

    final LayoutInflater inflater =
      NullCheck.notNull(this.getLayoutInflater());
    this.view = new ReaderTOCView(inflater, this, in_toc, this);
    this.setContentView(this.view.getLayoutView());
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    ReaderTOCActivity.LOG.debug("onDestroy");

    NullCheck.notNull(this.view).onTOCViewDestroy();
  }

  @Override public void onReaderSettingsChanged(
    final ReaderSettingsType s)
  {
    NullCheck.notNull(this.view).onReaderSettingsChanged(s);
  }

  @Override public void onTOCBackSelected()
  {
    this.finish();
  }

  @Override public void onTOCItemSelected(
    final TOCElement e)
  {
    final Intent intent = new Intent();
    intent.putExtra(ReaderTOCActivity.TOC_SELECTED_ID, e);
    this.setResult(Activity.RESULT_OK, intent);
    this.finish();
  }
}
