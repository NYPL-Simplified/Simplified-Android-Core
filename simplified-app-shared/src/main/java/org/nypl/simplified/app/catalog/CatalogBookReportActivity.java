package org.nypl.simplified.app.catalog;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.SimplifiedPart;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;

/**
 * An activity showing options for reporting
 */
public class CatalogBookReportActivity extends SimplifiedActivity
{
  private static final String FEED_ENTRY;

  static {
    FEED_ENTRY = "org.nypl.simplified.app.CatalogBookReportActivity.feed_entry";
  }

  private @Nullable BooksType books;
  private @Nullable FeedEntryOPDS feed_entry;
  private OptionType<CheckBox> current_check_box;

  /**
   * Construct an activity.
   */
  public CatalogBookReportActivity()
  {

  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return false;
  }

  @Override protected SimplifiedPart navigationDrawerGetPart()
  {
    return SimplifiedPart.PART_CATALOG;
  }

  private void configureUpButton()
  {
    final ActionBar bar = this.getActionBar();
    bar.setHomeAsUpIndicator(R.drawable.ic_drawer);
    bar.setDisplayHomeAsUpEnabled(true);
    bar.setHomeButtonEnabled(true);
    bar.setTitle(this.getResources().getString(R.string.catalog_book_report));
  }

  /**
   * Start a new reader for the given book.
   *
   * @param from        The parent activity
   * @param feed_entry  Feed entry of the book to report a problem with
   */
  public static void startActivity(
      final Activity from,
      final FeedEntryOPDS feed_entry)
  {
    NullCheck.notNull(feed_entry);
    final Bundle b = new Bundle();
    b.putSerializable(CatalogBookReportActivity.FEED_ENTRY, feed_entry);
    final Intent i = new Intent(from, CatalogBookReportActivity.class);
    i.putExtras(b);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    from.startActivity(i);
  }

  @Override protected void onResume()
  {
    super.onResume();
    this.configureUpButton();
  }

  @Override protected void onCreate(
      final @Nullable Bundle state)
  {
    super.onCreate(state);

    final SimplifiedCatalogAppServicesType cs = Simplified.getCatalogAppServices();
    this.books = cs.getBooks();

    final Intent intent = NullCheck.notNull(this.getIntent());
    final Bundle a = NullCheck.notNull(intent.getExtras());
    this.feed_entry = (FeedEntryOPDS) a.getSerializable(CatalogBookReportActivity.FEED_ENTRY);

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup layout = NullCheck.notNull(
        (ViewGroup) inflater.inflate(R.layout.catalog_book_report, content_area, false));
    content_area.addView(layout);
    content_area.requestLayout();

    final ViewGroup container = NullCheck.notNull(
      (ViewGroup) layout.findViewById(R.id.options_container)
    );

    this.current_check_box = Option.none();
    for (int i = 0; i < container.getChildCount(); i = i + 1) {
      final CheckBox check_box = NullCheck.notNull((CheckBox) container.getChildAt(i));
      if (check_box instanceof CheckBox) {
        check_box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
          @Override
          public void onCheckedChanged(final CompoundButton compound_button, final boolean b)
          {
            if (CatalogBookReportActivity.this.current_check_box.isSome()) {
              final Some<CheckBox> check_box_some = (Some<CheckBox>) CatalogBookReportActivity.this.current_check_box;
              final CheckBox check_box = check_box_some.get();
              if (check_box != compound_button) {
                check_box.setChecked(false);
              }
            }
            CatalogBookReportActivity.this.current_check_box = Option.some((CheckBox) compound_button);
          }
        });
      }
    }

    final Button submit_button = NullCheck.notNull(
      (Button) layout.findViewById(R.id.report_submit)
    );
    submit_button.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(final View view)
      {
        CatalogBookReportActivity.this.submitReport();
      }
    });
  }

  private void submitReport()
  {
    if (CatalogBookReportActivity.this.current_check_box.isSome()) {
      final Some<CheckBox> check_box_some = (Some<CheckBox>) CatalogBookReportActivity.this.current_check_box;
      final CheckBox check_box = check_box_some.get();
      if (check_box.isChecked()) {
        final String type = NullCheck.notNull((String) check_box.getTag());
        this.books.bookReport(this.feed_entry, type);
        this.finish();
      }
    }
  }
}
