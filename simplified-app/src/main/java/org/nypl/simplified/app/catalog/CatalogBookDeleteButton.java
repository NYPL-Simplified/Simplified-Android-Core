package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogBookDeleteButton extends Button implements
  CatalogBookButtonType
{
  public CatalogBookDeleteButton(
    final Activity in_activity,
    final BookID in_book_id)
  {
    super(in_activity);

    final SimplifiedCatalogAppServicesType ss =
      Simplified.getCatalogAppServices();
    final LinearLayout.LayoutParams lp =
      new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.leftMargin = (int) ss.screenDPToPixels(8);
    this.setLayoutParams(lp);

    final Resources rr = NullCheck.notNull(in_activity.getResources());
    this
      .setText(NullCheck.notNull(rr.getString(R.string.catalog_book_delete)));
    this.setTextSize(12.0f);
    this.setBackground(rr.getDrawable(R.drawable.simplified_button));
    this
      .setTextColor(rr.getColorStateList(R.drawable.simplified_button_text));

    this.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        final CatalogBookDeleteDialog d = CatalogBookDeleteDialog.newDialog();
        d.setOnConfirmListener(new Runnable() {
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
