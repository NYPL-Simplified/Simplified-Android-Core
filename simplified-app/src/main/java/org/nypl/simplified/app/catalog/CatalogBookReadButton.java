package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.content.res.Resources;
import android.widget.Button;
import android.widget.LinearLayout;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.books.core.BookID;

/**
 * A button that opens a given book for reading.
 */

public final class CatalogBookReadButton extends Button
  implements CatalogBookButtonType
{
  /**
   * The parent activity.
   *
   * @param in_activity The activity
   * @param in_book_id  The book ID
   */

  public CatalogBookReadButton(
    final Activity in_activity,
    final BookID in_book_id)
  {
    super(in_activity);

    final SimplifiedCatalogAppServicesType ss =
      Simplified.getCatalogAppServices();
    final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
      android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
      android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.leftMargin = (int) ss.screenDPToPixels(8);
    this.setLayoutParams(lp);

    final Resources rr = NullCheck.notNull(in_activity.getResources());
    this.setText(NullCheck.notNull(rr.getString(R.string.catalog_book_read)));
    this.setTextSize(12.0f);
    this.setBackground(rr.getDrawable(R.drawable.simplified_button));
    this.setTextColor(rr.getColorStateList(R.drawable.simplified_button_text));
    this.setOnClickListener(new CatalogBookRead(in_activity, in_book_id));
  }
}
