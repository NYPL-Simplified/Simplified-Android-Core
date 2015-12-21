package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.view.View;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;

/**
 * A button for deleting books.
 */

public final class CatalogBookDeleteButton extends CatalogLeftPaddedButton
  implements CatalogBookButtonType
{
  /**
   * Construct a button.
   *
   * @param in_activity The host activity
   * @param in_book_id  The book ID
   */

  public CatalogBookDeleteButton(
    final Activity in_activity,
    final BookID in_book_id)
  {
    super(in_activity);

    final Resources rr = NullCheck.notNull(in_activity.getResources());
    this.getTextView().setText(NullCheck.notNull(rr.getString(R.string.catalog_book_delete)));
    this.getTextView().setTextSize(12.0f);
    this.setBackground(rr.getDrawable(R.drawable.simplified_button));
    this.getTextView().setTextColor(rr.getColorStateList(R.drawable.simplified_button_text));

    this.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          final CatalogBookDeleteDialog d = CatalogBookDeleteDialog.newDialog();
          d.setOnConfirmListener(
            new Runnable()
            {
              @Override public void run()
              {
                final SimplifiedCatalogAppServicesType app =
                  Simplified.getCatalogAppServices();
                final BooksType books = app.getBooks();
                books.bookDeleteData(in_book_id);
              }
            });
          final FragmentManager fm = in_activity.getFragmentManager();
          d.show(fm, "delete-confirm");
        }
      });
  }
}
