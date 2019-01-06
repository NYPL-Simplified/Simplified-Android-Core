package org.nypl.simplified.app.catalog;

import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;

import com.io7m.jnull.NullCheck;

import org.nypl.simplified.app.R;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.controller.BooksControllerType;

/**
 * A button for deleting books.
 */

public final class CatalogBookDeleteButton
  extends AppCompatButton implements CatalogBookButtonType {

  /**
   * Construct a button.
   */

  public CatalogBookDeleteButton(
    final AppCompatActivity activity,
    final BooksControllerType booksController,
    final AccountType account,
    final BookID bookID) {

    super(activity);

    final Resources resources = NullCheck.notNull(activity.getResources());

    this.setText(NullCheck.notNull(resources.getString(R.string.catalog_book_delete)));
    this.setContentDescription(NullCheck.notNull(resources.getString(R.string.catalog_accessibility_book_delete)));
    this.setTextSize(12.0f);

    this.setOnClickListener(view -> {
      final CatalogBookDeleteDialog d = CatalogBookDeleteDialog.newDialog();
      d.setOnConfirmListener(() -> booksController.bookDelete(account, bookID));
      final android.support.v4.app.FragmentManager fm = activity.getSupportFragmentManager();
      d.show(fm, "delete-confirm");
    });
  }
}
