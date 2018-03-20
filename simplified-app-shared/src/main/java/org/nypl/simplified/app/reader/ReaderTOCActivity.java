package org.nypl.simplified.app.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;
import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

/**
 * Activity for displaying the table of contents on devices with small screens.
 */

public final class ReaderTOCActivity extends AppCompatActivity
  implements ReaderSettingsListenerType, ReaderTOCViewSelectionListenerType
{
  /**
   * The name of the argument containing the TOC.
   */

  public static final String TOC_ID;

  /**
   * The name of the argument containing the selected TOC item.
   */

  public static final String TOC_SELECTED_ID;

  /**
   * The activity request code (for retrieving the result of executing the
   * activity).
   */

  public static final int TOC_SELECTION_REQUEST_CODE;

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderTOCActivity.class);
    TOC_SELECTION_REQUEST_CODE = 23;
    TOC_ID = "org.nypl.simplified.app.reader.ReaderTOCActivity.toc";
    TOC_SELECTED_ID =
      "org.nypl.simplified.app.reader.ReaderTOCActivity.toc_selected";
  }

  private @Nullable ReaderTOCView view;
  public @Nullable ReaderTOC in_toc;

  /**
   * Construct an activity.
   */

  public ReaderTOCActivity()
  {

  }

  /**
   * Start a TOC activity. The user will be prompted to select a TOC item, and
   * the results of that selection will be reported using the request code
   * {@link #TOC_SELECTION_REQUEST_CODE}.
   *
   * @param from The parent activity
   * @param toc  The table of contents
   */

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
      i, ReaderTOCActivity.TOC_SELECTION_REQUEST_CODE);
  }

  @Override public void finish()
  {
    super.finish();
    this.overridePendingTransition(0, 0);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    this.setTitle("Table of Contents");

    //TODO WIP

    //TODO Deleted theme setting stuff

    super.onCreate(state);

    ReaderTOCActivity.LOG.debug("onCreate");

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderSettingsType settings = rs.getSettings();
    settings.addListener(this);

    final Intent input = NullCheck.notNull(this.getIntent());
    final Bundle args = NullCheck.notNull(input.getExtras());

    this.in_toc = NullCheck.notNull(
      (ReaderTOC) args.getSerializable(ReaderTOCActivity.TOC_ID));

//    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());


    //TODO WIP
    this.setContentView(R.layout.reader_toc_tab_layout);

    ViewPager pager = findViewById(R.id.reader_toc_view_pager);
    final ReaderTOCFragmentPagerAdapter adapter = new ReaderTOCFragmentPagerAdapter(getSupportFragmentManager());
    pager.setAdapter(adapter);

    TabLayout tabLayout = findViewById(R.id.reader_toc_tab_layout);
    tabLayout.setupWithViewPager(pager);

//    this.view = new ReaderTOCView(inflater, this, in_toc, this);
//
//    this.setContentView(this.view.getLayoutView());
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
