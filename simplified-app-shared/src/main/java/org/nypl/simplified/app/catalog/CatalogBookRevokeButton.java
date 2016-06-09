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
 * A button for revoking loans or holds.
 */

public final class CatalogBookRevokeButton extends CatalogLeftPaddedButton
  implements CatalogBookButtonType
{
  /**
   * Construct a button.
   *
   * @param in_activity    The host activity
   * @param in_book_id     The book ID
   * @param in_revoke_type The revocation type (to show the correct button text
   *                       and dialog messages)
   */

  public CatalogBookRevokeButton(
    final Activity in_activity,
    final BookID in_book_id,
    final CatalogBookRevokeType in_revoke_type)
  {
    super(in_activity);
    NullCheck.notNull(in_book_id);
    NullCheck.notNull(in_revoke_type);

    final Resources rr = NullCheck.notNull(in_activity.getResources());
    switch (in_revoke_type) {
      case REVOKE_LOAN: {
        this.getTextView().setText(
          NullCheck.notNull(rr.getString(R.string.catalog_book_revoke_loan)));
        this.getTextView().setContentDescription(
          NullCheck.notNull(rr.getString(R.string.catalog_accessibility_book_revoke_loan)));
        break;
      }
      case REVOKE_HOLD: {
        this.getTextView().setText(
          NullCheck.notNull(rr.getString(R.string.catalog_book_revoke_hold)));
        this.getTextView().setContentDescription(
          NullCheck.notNull(rr.getString(R.string.catalog_accessibility_book_revoke_hold)));
        break;
      }
    }

    this.getTextView().setTextSize(12.0f);
    this.setBackground(rr.getDrawable(R.drawable.simplified_button));
    this.getTextView().setTextColor(rr.getColorStateList(R.drawable.simplified_button_text));

    this.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          final CatalogBookRevokeDialog d =
            CatalogBookRevokeDialog.newDialog(in_revoke_type);
          d.setOnConfirmListener(
            new Runnable()
            {
              @Override public void run()
              {
                final SimplifiedCatalogAppServicesType app =
                  Simplified.getCatalogAppServices();
                final BooksType books = app.getBooks();
                books.bookRevoke(in_book_id);
              }
            });
          final FragmentManager fm = in_activity.getFragmentManager();
          d.show(fm, "revoke-confirm");
        }
      });
  }
}
