package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.content.res.Resources;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.app.R;
import org.nypl.simplified.books.core.FeedEntryOPDS;

/**
 * A button that opens a report dialog when
 */
public final class CatalogBookReportButton extends CatalogLeftPaddedButton
  implements CatalogBookButtonType
{
    /**
     * The parent activity.
     *
     * @param in_activity The activity
     * @param in_feed_entry Book feed entry, to get URI to submit report to
     */

    public CatalogBookReportButton(
            final Activity in_activity,
            final FeedEntryOPDS in_feed_entry)
    {
        super(in_activity);

        final Resources rr = NullCheck.notNull(in_activity.getResources());
        this.setText(NullCheck.notNull(rr.getString(R.string.catalog_book_report)));
        this.setTextSize(12.0f);
        this.setBackground(rr.getDrawable(R.drawable.simplified_button));
        this.setTextColor(rr.getColorStateList(R.drawable.simplified_button_text));
        this.setOnClickListener(new CatalogBookReport(in_activity, in_feed_entry));
    }
}
