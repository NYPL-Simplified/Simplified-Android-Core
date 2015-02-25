package org.nypl.simplified.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.res.Resources;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class Parts implements DrawerListener, OnItemClickListener
{
  private final MainActivity          activity;
  private final ArrayList<String>     drawer_items;
  private final PartBooks             part_books;
  private final PartCatalog           part_catalog;
  private final PartHolds             part_holds;
  private final PartSettings          part_settings;
  private final Map<String, PartType> parts;

  public Parts(
    final MainActivity act)
  {
    this.activity = NullCheck.notNull(act);

    final Resources rr = NullCheck.notNull(this.activity.getResources());

    final DrawerLayout drawer_layout =
      NullCheck.notNull((DrawerLayout) this.activity
        .findViewById(R.id.drawer_layout));
    drawer_layout.setDrawerListener(this);
    drawer_layout.openDrawer(GravityCompat.START);

    final ListView drawer_list =
      NullCheck.notNull((ListView) this.activity
        .findViewById(R.id.left_drawer));

    final String catalog_name = rr.getString(R.string.catalog);
    final String holds_name = rr.getString(R.string.holds);
    final String books_name = rr.getString(R.string.books);
    final String settings_name = rr.getString(R.string.settings);

    this.drawer_items = new ArrayList<String>();
    this.drawer_items.add(catalog_name);
    this.drawer_items.add(holds_name);
    this.drawer_items.add(books_name);
    this.drawer_items.add(settings_name);

    this.part_catalog = new PartCatalog(act);
    this.part_holds = new PartHolds(act);
    this.part_books = new PartBooks(act);
    this.part_settings = new PartSettings(act);

    this.parts = new HashMap<String, PartType>();
    this.parts.put(catalog_name, this.part_catalog);
    this.parts.put(holds_name, this.part_holds);
    this.parts.put(books_name, this.part_books);
    this.parts.put(settings_name, this.part_settings);

    drawer_list.setAdapter(new ArrayAdapter<String>(
      this.activity,
      R.layout.drawer_item,
      this.drawer_items));
    drawer_list.setOnItemClickListener(this);

    this.part_catalog.partSwitchTo();
  }

  @Override public void onDrawerClosed(
    final @Nullable View drawerView)
  {

  }

  @Override public void onDrawerOpened(
    final @Nullable View drawerView)
  {

  }

  @Override public void onDrawerSlide(
    final @Nullable View drawerView,
    final float slideOffset)
  {

  }

  @Override public void onDrawerStateChanged(
    final int newState)
  {

  }

  @Override public void onItemClick(
    final @Nullable AdapterView<?> parent,
    final @Nullable View view,
    final int position,
    final long id)
  {
    final String name = NullCheck.notNull(this.drawer_items.get(position));
    final PartType part = NullCheck.notNull(this.parts.get(name));
    part.partSwitchTo();
  }
}
