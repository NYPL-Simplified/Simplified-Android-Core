package org.nypl.simplified.app.catalog;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.NavigationDrawerActivity;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.books.feeds.FeedEntryOPDS;

/**
 * An activity showing options for reporting
 */

public class CatalogBookReportActivity extends NavigationDrawerActivity {

  private static final String FEED_ENTRY =
    "org.nypl.simplified.app.CatalogBookReportActivity.feed_entry";

  private FeedEntryOPDS feed_entry;
  private OptionType<CheckBox> current_check_box;

  /**
   * Construct an activity.
   */
  public CatalogBookReportActivity() {

  }

  @Override
  protected boolean navigationDrawerShouldShowIndicator() {
    return false;
  }

  @Override
  protected String navigationDrawerGetActivityTitle(final Resources resources) {
    return resources.getString(R.string.catalog);
  }

  @Override
  public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn) {
    final MenuItem item = NullCheck.notNull(item_mn);
    switch (item.getItemId()) {

      case android.R.id.home: {
        onBackPressed();
        return true;
      }

      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }

  private void configureUpButton() {
    final ActionBar bar = this.getActionBar();
    bar.setTitle(this.getResources().getString(R.string.catalog_book_report));
    if (android.os.Build.VERSION.SDK_INT < 21) {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(true);
      bar.setIcon(R.drawable.ic_arrow_back);
    } else {
      bar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(false);
    }
  }

  /**
   * Start a new reader for the given book.
   *
   * @param from       The parent activity
   * @param feed_entry Feed entry of the book to report a problem with
   */
  public static void startActivity(
    final Activity from,
    final FeedEntryOPDS feed_entry) {
    NullCheck.notNull(feed_entry);
    final Bundle b = new Bundle();
    b.putSerializable(CatalogBookReportActivity.FEED_ENTRY, feed_entry);
    final Intent i = new Intent(from, CatalogBookReportActivity.class);
    i.putExtras(b);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    from.startActivity(i);
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.configureUpButton();
  }

  @Override
  protected void onCreate(final @Nullable Bundle state) {
    super.onCreate(state);

    final Intent intent = NullCheck.notNull(this.getIntent());
    final Bundle a = NullCheck.notNull(intent.getExtras());
    this.feed_entry = (FeedEntryOPDS) a.getSerializable(CatalogBookReportActivity.FEED_ENTRY);

    final LayoutInflater inflater =
      NullCheck.notNull(this.getLayoutInflater());

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(R.layout.catalog_book_report, content_area, false));
    content_area.addView(layout);
    content_area.requestLayout();

    final ViewGroup container =
      NullCheck.notNull(layout.findViewById(R.id.options_container));

    this.current_check_box = Option.none();
    for (int i = 0; i < container.getChildCount(); i = i + 1) {
      final CheckBox check_box = NullCheck.notNull((CheckBox) container.getChildAt(i));
      if (check_box instanceof CheckBox) {
        check_box.setOnCheckedChangeListener((compound_button, b) -> {
          if (this.current_check_box.isSome()) {
            final Some<CheckBox> check_box_some = (Some<CheckBox>) this.current_check_box;
            final CheckBox check_box1 = check_box_some.get();
            if (check_box1 != compound_button) {
              check_box1.setChecked(false);
            }
          }
          this.current_check_box = Option.some((CheckBox) compound_button);
        });
      }
    }

    final Button submit_button =
      NullCheck.notNull(layout.findViewById(R.id.report_submit));

    submit_button.setOnClickListener(view -> this.submitReport());
  }

  private void submitReport() {
    if (this.current_check_box.isSome()) {
      final Some<CheckBox> check_box_some = (Some<CheckBox>) this.current_check_box;
      final CheckBox check_box = check_box_some.get();
      if (check_box.isChecked()) {
        final String type = NullCheck.notNull((String) check_box.getTag());
        Simplified.getBooksController().bookReport(this.feed_entry, type);
        this.finish();
      }
    }
  }
}
