package org.nypl.simplified.app.catalog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.R;
import org.nypl.simplified.feeds.api.FeedFacetType;

import java.util.ArrayList;

/**
 * A dialog used to select facets.
 */

public final class CatalogFacetDialog extends DialogFragment
  implements OnItemClickListener
{
  private static final String GROUP_ID;
  private static final String GROUP_NAME_ID;

  static {
    GROUP_ID = "org.nypl.simplified.app.catalog.CatalogFacetDialog.facets";
    GROUP_NAME_ID =
      "org.nypl.simplified.app.catalog.CatalogFacetDialog.facets_name";
  }

  private @Nullable ArrayList<FeedFacetType>          group;
  private @Nullable ArrayAdapter<String>              group_adapter;
  private @Nullable String                            group_name;
  private @Nullable CatalogFacetSelectionListenerType listener;

  /**
   * Construct a dialog.
   */

  public CatalogFacetDialog()
  {
    // Fragments must have no-arg constructors.
  }

  /**
   * Construct a dialog.
   *
   * @param in_facet_group_name The facet group name
   * @param in_facet_group      The facet group
   *
   * @return A new dialog
   */

  public static CatalogFacetDialog newDialog(
    final String in_facet_group_name,
    final ArrayList<FeedFacetType> in_facet_group)
  {
    NullCheck.notNull(in_facet_group);
    final CatalogFacetDialog c = new CatalogFacetDialog();
    final Bundle b = new Bundle();
    b.putString(CatalogFacetDialog.GROUP_NAME_ID, in_facet_group_name);
    b.putSerializable(CatalogFacetDialog.GROUP_ID, in_facet_group);
    c.setArguments(b);
    return c;
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final Bundle b = NullCheck.notNull(this.getArguments());

    @SuppressWarnings("unchecked") final ArrayList<FeedFacetType> in_group =
      NullCheck.notNull(
        (ArrayList<FeedFacetType>) b.getSerializable(
          CatalogFacetDialog.GROUP_ID));

    this.group = in_group;
    this.group_name =
      NullCheck.notNull(b.getString(CatalogFacetDialog.GROUP_NAME_ID));

    final ArrayList<String> in_strings = new ArrayList<String>(in_group.size());
    for (final FeedFacetType f : in_group) {
      in_strings.add(f.facetGetTitle());
    }

    this.group_adapter = new ArrayAdapter<String>(
      this.getActivity(),
      android.R.layout.simple_list_item_1,
      android.R.id.text1,
      in_strings);
  }

  @Override public void onResume()
  {
    super.onResume();

    final Resources rr = NullCheck.notNull(this.getResources());
    final int h = (int) rr.getDimension(R.dimen.facet_dialog_height);
    final int w = (int) rr.getDimension(R.dimen.facet_dialog_width);

    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(w, h);
    window.setGravity(Gravity.CENTER);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater_mn,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);

    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.facet_dialog, container, false));

    final ListView in_list =
      NullCheck.notNull((ListView) layout.findViewById(R.id.facet_list));
    final TextView in_title =
      NullCheck.notNull((TextView) layout.findViewById(R.id.facet_title));

    in_list.setAdapter(this.group_adapter);
    in_list.setOnItemClickListener(this);
    in_title.setText(this.group_name + ":");

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }
    return layout;
  }

  @Override public void onItemClick(
    final @Nullable AdapterView<?> av,
    final @Nullable View v,
    final int position,
    final long id)
  {
    final FeedFacetType f =
      NullCheck.notNull(NullCheck.notNull(this.group).get(position));
    NullCheck.notNull(this.listener).onFacetSelected(f);
  }

  /**
   * Set the selection listener.
   *
   * @param in_listener The listener
   */

  public void setFacetSelectionListener(
    final CatalogFacetSelectionListenerType in_listener)
  {
    this.listener = NullCheck.notNull(in_listener);
  }
}
