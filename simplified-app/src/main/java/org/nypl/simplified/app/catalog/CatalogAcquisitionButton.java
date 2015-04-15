package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.R;
import org.nypl.simplified.opds.core.OPDSAcquisition;

import android.content.Context;
import android.content.res.Resources;
import android.widget.Button;

import com.io7m.jnull.NullCheck;

public final class CatalogAcquisitionButton extends Button
{
  private final OPDSAcquisition acquisition;

  public CatalogAcquisitionButton(
    final Context in_context,
    final OPDSAcquisition in_acq)
  {
    super(in_context);
    this.acquisition = NullCheck.notNull(in_acq);

    final Resources rr = NullCheck.notNull(in_context.getResources());
    switch (this.acquisition.getType()) {
      case ACQUISITION_BORROW:
      {
        this.setText(rr.getString(R.string.catalog_book_borrow));
        break;
      }
      case ACQUISITION_BUY:
      {
        this.setText(rr.getString(R.string.catalog_book_buy));
        break;
      }
      case ACQUISITION_GENERIC:
      {
        this.setText(rr.getString(R.string.catalog_book_generic));
        break;
      }
      case ACQUISITION_OPEN_ACCESS:
      {
        this.setText(rr.getString(R.string.catalog_book_open_access));
        break;
      }
      case ACQUISITION_SAMPLE:
      {
        this.setText(rr.getString(R.string.catalog_book_sample));
        break;
      }
      case ACQUISITION_SUBSCRIBE:
      {
        this.setText(rr.getString(R.string.catalog_book_subscribe));
        break;
      }
    }
  }
}
