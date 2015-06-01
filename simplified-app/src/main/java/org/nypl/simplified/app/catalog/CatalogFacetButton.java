package org.nypl.simplified.app.catalog;

import java.util.ArrayList;

import org.nypl.simplified.app.R;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.core.FeedFacetType;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.view.View;
import android.widget.Button;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogFacetButton extends Button
{
  public CatalogFacetButton(
    final Activity in_activity,
    final String in_group_name,
    final ArrayList<FeedFacetType> in_group,
    final CatalogFacetSelectionListenerType in_listener)
  {
    super(in_activity);

    NullCheck.notNull(in_group);
    NullCheck.notNull(in_group_name);
    NullCheck.notNull(in_listener);

    Assertions.checkPrecondition(
      in_group.isEmpty() == false,
      "Facet group is not empty");

    FeedFacetType active_maybe = NullCheck.notNull(in_group.get(0));
    for (final FeedFacetType f : in_group) {
      if (f.facetIsActive()) {
        active_maybe = NullCheck.notNull(f);
        break;
      }
    }

    final FeedFacetType active = NullCheck.notNull(active_maybe);
    final Resources rr = NullCheck.notNull(in_activity.getResources());
    this.setTextSize(12.0f);
    this.setBackground(rr.getDrawable(R.drawable.simplified_button));
    this
      .setTextColor(rr.getColorStateList(R.drawable.simplified_button_text));
    this.setText(active.facetGetTitle());
    this.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        final FragmentManager fm = in_activity.getFragmentManager();
        final CatalogFacetDialog d =
          CatalogFacetDialog.newDialog(in_group_name, in_group);
        d.setFacetSelectionListener(new CatalogFacetSelectionListenerType() {
          @Override public void onFacetSelected(
            final FeedFacetType f)
          {
            d.dismiss();
            in_listener.onFacetSelected(f);
          }
        });
        d.show(fm, "facet-dialog");
      }
    });
  }
}
