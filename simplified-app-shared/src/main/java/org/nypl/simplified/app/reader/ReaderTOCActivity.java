package org.nypl.simplified.app.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;
import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;
import org.nypl.simplified.books.core.BookmarkAnnotation;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Activity for displaying the ViewPager which contains Fragments
 * for the Table of Contents and User-Saved Bookmarks.
 */

public final class ReaderTOCActivity extends AppCompatActivity
  implements ReaderSettingsListenerType, ReaderTOCContentsFragmentSelectionListenerType,
    ReaderTOCBookmarksFragmentSelectionListenerType
{
  /**
   * The name of the argument containing the TOC.
   */

  public static final String TOC_ID;

  /**
   * The name of the argument containing the user bookmarks.
   */

  public static final String BOOKMARKS_ID;

  /**
   * The name of the argument containing the selected TOC item.
   */

  public static final String TOC_SELECTED_ID;

  /**
   * The name of the argument containing the selected TOC item.
   */

  public static final String BOOKMARK_SELECTED_ID;

  /**
   * The activity request code (for retrieving the result of executing the
   * activity).
   */

  public static final int TOC_SELECTION_REQUEST_CODE;

  /**
   * The activity request code (for retrieving the result of executing the
   * activity).
   */

  public static final int BOOKMARK_SELECTION_REQUEST_CODE;


  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderTOCActivity.class);
    TOC_SELECTION_REQUEST_CODE = 23;
    //TODO any rhyme or reason to this number?
    BOOKMARK_SELECTION_REQUEST_CODE = 24;
    TOC_ID = "org.nypl.simplified.app.reader.ReaderTOCActivity.toc";
    TOC_SELECTED_ID = "org.nypl.simplified.app.reader.ReaderTOCActivity.toc_selected";
    BOOKMARKS_ID = "org.nypl.simplified.app.reader.ReaderTOCActivity.bookmarks";
    BOOKMARK_SELECTED_ID = "org.nypl.simplified.app.reader.ReaderTOCActivity.bookmark_selected";
  }

  public @Nullable ReaderTOC in_toc;
  public @Nullable List<BookmarkAnnotation> bookmarks;

  /**
   * Construct an activity.
   */

  public ReaderTOCActivity()
  {

  }

  /**
   * Start a TOC activity. The user will be shown a pager view.
   * If they select a TOC or Bookmark item, the results of that selection
   * will be reported using the request code {@link #TOC_SELECTION_REQUEST_CODE}.
   *
   * @param from The parent activity
   * @param toc  The table of contents
   */

  public static void startActivityForResult(
    final Activity from,
    final ReaderTOC toc,
    final List<BookmarkAnnotation> marks)
  {
    Objects.requireNonNull(from);
    Objects.requireNonNull(toc);

    final Intent i = new Intent(Intent.ACTION_PICK);
    i.setClass(from, ReaderTOCActivity.class);
    i.putExtra(ReaderTOCActivity.TOC_ID, toc);
    i.putExtra(ReaderTOCActivity.BOOKMARKS_ID, (Serializable) marks);

    //TODO need to figure out how to handle Activity Result now that there are two possible finishes

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
    super.onCreate(state);
    ReaderTOCActivity.LOG.debug("onCreate");

    this.setTitle(R.string.reader_toc);

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderSettingsType settings = rs.getSettings();
    settings.addListener(this);

    final Intent input = NullCheck.notNull(this.getIntent());
    final Bundle args = NullCheck.notNull(input.getExtras());

    this.in_toc = NullCheck.notNull(
      (ReaderTOC) args.getSerializable(ReaderTOCActivity.TOC_ID));

    this.bookmarks = NullCheck.notNull(
      (List<BookmarkAnnotation>) args.getSerializable(ReaderTOCActivity.BOOKMARKS_ID));

    this.setContentView(R.layout.reader_toc_tab_layout);

    ViewPager pager = findViewById(R.id.reader_toc_view_pager);
    final ReaderTOCFragmentPagerAdapter pagerAdapter = new ReaderTOCFragmentPagerAdapter(getSupportFragmentManager());
    pager.setAdapter(pagerAdapter);

    TabLayout tabLayout = findViewById(R.id.reader_toc_tab_layout);
    tabLayout.setupWithViewPager(pager);
  }

  @Override public void onReaderSettingsChanged(
    final ReaderSettingsType s)
  {
    final ReaderTOCContentsFragment contentsFragment =
        (ReaderTOCContentsFragment) getSupportFragmentManager().findFragmentById(R.id.reader_toc);

    if (contentsFragment != null) {
      contentsFragment.onReaderSettingsChanged(s);
    }
  }

  /**
   * ReaderTOCContentsFragmentSelectionListener Methods
   */

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

  /**
   * ReaderTOCBookmarksFragmentSelectionListener Methods
   */

  @Override
  public void onBookmarkSelected(BookmarkAnnotation bookmark) {
    final Intent intent = new Intent();
    intent.putExtra(ReaderTOCActivity.BOOKMARK_SELECTED_ID, bookmark);
    this.setResult(Activity.RESULT_OK, intent);
    this.finish();
  }
}
