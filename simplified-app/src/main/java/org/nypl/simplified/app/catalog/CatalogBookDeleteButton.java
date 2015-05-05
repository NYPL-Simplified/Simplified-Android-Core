package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.R;
import org.nypl.simplified.books.core.BookID;

import android.app.Activity;
import android.content.res.Resources;
import android.widget.Button;

import com.io7m.jnull.NullCheck;

public final class CatalogBookDeleteButton extends Button
{
  public CatalogBookDeleteButton(
    final Activity in_activity,
    final BookID in_book_id)
  {
    super(in_activity);

    final Resources rr = NullCheck.notNull(in_activity.getResources());
    this
      .setText(NullCheck.notNull(rr.getString(R.string.catalog_book_delete)));
    this.setTextSize(12.0f);
    this.setBackground(rr.getDrawable(R.drawable.simplified_button));
    this
      .setTextColor(rr.getColorStateList(R.drawable.simplified_button_text));
    this.setOnClickListener(new CatalogBookDelete(in_book_id));
  }
}
