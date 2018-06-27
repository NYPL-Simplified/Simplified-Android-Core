package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.content.res.Resources;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.ThemeMatcher;
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DeviceActivationListenerType;
import org.nypl.simplified.books.core.FeedEntryOPDS;


/**
 * A button that opens a given book for reading.
 */

public final class CatalogBookReadButton extends CatalogLeftPaddedButton
  implements CatalogBookButtonType
{
  /**
   * The parent activity.
   *
   * @param in_activity The activity
   * @param in_book_id  The book ID
   * @param in_entry    The associated feed entry
   * @param in_books    books
   */

  public CatalogBookReadButton(
    final Activity in_activity,
    final BookID in_book_id,
    final FeedEntryOPDS in_entry,
    final BooksType in_books)
  {
    super(in_activity);


    final Resources rr = NullCheck.notNull(in_activity.getResources());
    this.getTextView().setTextSize(12.0f);

    this.setBackgroundResource(R.drawable.simplified_button);
    final int resID = ThemeMatcher.Companion.color(Simplified.getCurrentAccount().getMainColor());
    final int mainColor = ContextCompat.getColor(this.getContext(), resID);
    this.getTextView().setTextColor(mainColor);

    final OptionType<BookDatabaseEntrySnapshot> snap_opt =
      in_books.bookGetDatabase().databaseGetEntrySnapshot(in_book_id);

    if (in_books.accountIsDeviceActive() || ((Some<BookDatabaseEntrySnapshot>) snap_opt).get().getAdobeRights().isNone()) {

      this.getTextView().setText(NullCheck.notNull(rr.getString(R.string.catalog_book_read)));
      this.getTextView().setContentDescription(NullCheck.notNull(rr.getString(R.string.catalog_accessibility_book_read)));

      this.setOnClickListener(new CatalogBookRead(in_activity, in_book_id, in_entry));
    }
    else
    {
      this.getTextView().setText(NullCheck.notNull(rr.getString(R.string.catalog_book_download)));
      this.getTextView().setContentDescription(NullCheck.notNull(rr.getString(R.string.catalog_accessibility_book_download)));

      this.setOnClickListener(

        new OnClickListener() {
          @Override
          public void onClick(
            final @Nullable View v) {


            DeviceActivationListenerType listener = new DeviceActivationListenerType() {
              @Override
              public void onDeviceActivationFailure(final String message) {

              }
              @Override
              public void onDeviceActivationSuccess() {

              }
            };

            in_books.accountActivateDeviceAndFulFillBook(in_book_id, in_entry.getFeedEntry().getLicensor(), listener);

          }
        }
      );
    }


  }
}
