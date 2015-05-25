package org.nypl.simplified.app.catalog;

import java.util.ArrayList;
import java.util.List;

import org.nypl.simplified.app.R;
import org.nypl.simplified.opds.core.OPDSFacet;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogFacetMenuController implements
  android.widget.AdapterView.OnItemSelectedListener
{
  private final List<OPDSFacet>              facets;
  private final List<String>                 texts;
  private final ArrayAdapter<String>         adapter;
  private final CatalogFacetMenuListenerType listener;
  private boolean                            first;

  public CatalogFacetMenuController(
    final Context context,
    final Spinner in_spinner,
    final List<OPDSFacet> in_facets,
    final CatalogFacetMenuListenerType in_listener)
  {
    NullCheck.notNull(context);
    NullCheck.notNull(in_spinner);
    this.facets = NullCheck.notNull(in_facets);
    this.listener = NullCheck.notNull(in_listener);
    this.texts = new ArrayList<String>(this.facets.size());

    for (final OPDSFacet f : this.facets) {
      this.texts.add(f.getTitle());
    }

    this.adapter =
      new ArrayAdapter<String>(
        context,
        R.layout.facet_dropdown_item,
        this.texts);

    in_spinner.setAdapter(this.adapter);

    for (int index = 0; index < this.facets.size(); ++index) {
      final OPDSFacet f = NullCheck.notNull(this.facets.get(index));
      if (f.isActive()) {
        in_spinner.setSelection(index);
      }
    }

    this.first = true;
    in_spinner.setOnItemSelectedListener(this);
  }

  @Override public void onItemSelected(
    final @Nullable AdapterView<?> parent,
    final @Nullable View view,
    final int position,
    final long id)
  {
    /**
     * For some reason, Android calls this method once when the spinner is
     * created, so it's necessary to ignore the very first call.
     */

    if (this.first == false) {
      final OPDSFacet f = NullCheck.notNull(this.facets.get(position));
      this.listener.onFacetSelected(f);
    }
    this.first = false;
  }

  @Override public void onNothingSelected(
    final @Nullable AdapterView<?> parent)
  {
    this.listener.onFacetSelectedNone();
  }
}
