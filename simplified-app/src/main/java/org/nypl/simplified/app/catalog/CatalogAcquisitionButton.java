package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.R;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.opds.core.OPDSAcquisition;

import android.app.Activity;
import android.content.res.Resources;
import android.widget.Button;

import com.io7m.jnull.NullCheck;

public final class CatalogAcquisitionButton extends Button
{
  public CatalogAcquisitionButton(
    final Activity in_activity,
    final BooksType in_books,
    final BookID in_book_id,
    final OPDSAcquisition in_acq,
    final FeedEntryOPDS in_entry)
  {
    super(in_activity);

    final Resources rr = NullCheck.notNull(in_activity.getResources());
    this.setText(NullCheck.notNull(rr
      .getString(R.string.catalog_book_download)));
    this.setTextSize(12.0f);
    this.setBackground(rr.getDrawable(R.drawable.simplified_button));
    this
      .setTextColor(rr.getColorStateList(R.drawable.simplified_button_text));
    this.setOnClickListener(new CatalogAcquisitionButtonController(
      in_activity,
      in_books,
      in_book_id,
      in_acq,
      in_entry));
  }
}
