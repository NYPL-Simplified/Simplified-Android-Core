package org.nypl.simplified.app;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class MainActivity extends Activity
{
  private int part_current;

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.main);

    final DrawerLayout in_drawer =
      NullCheck.notNull((DrawerLayout) this.findViewById(R.id.drawer_layout));
    final ListView in_drawer_list =
      NullCheck.notNull((ListView) this.findViewById(R.id.left_drawer));
    final FrameLayout in_content =
      NullCheck.notNull((FrameLayout) this.findViewById(R.id.content_frame));

    final FragmentManager fm = NullCheck.notNull(this.getFragmentManager());
    final Resources rr = NullCheck.notNull(this.getResources());

    final String catalog_name =
      NullCheck.notNull(rr.getString(R.string.catalog));
    final String holds_name = NullCheck.notNull(rr.getString(R.string.holds));
    final String books_name = NullCheck.notNull(rr.getString(R.string.books));
    final String settings_name =
      NullCheck.notNull(rr.getString(R.string.settings));

    final PartBooks part_books = PartBooks.newInstance();
    final PartCatalog part_catalog = PartCatalog.newInstance(fm);
    final PartHolds part_holds = PartHolds.newInstance();
    final PartSettings part_settings = PartSettings.newInstance();

    final List<PartType> in_parts = new ArrayList<PartType>();
    final List<String> in_titles = new ArrayList<String>();
    in_titles.add(catalog_name);
    in_parts.add(part_catalog);
    in_titles.add(holds_name);
    in_parts.add(part_holds);
    in_titles.add(books_name);
    in_parts.add(part_books);
    in_titles.add(settings_name);
    in_parts.add(part_settings);

    this.part_current = 0;

    in_drawer_list.setAdapter(new ArrayAdapter<String>(
      this,
      R.layout.drawer_item,
      in_titles));

    /**
     * The default part is the catalog.
     */

    {
      final Fragment fragment = part_catalog.getCurrentFragment();
      final FragmentTransaction ft = fm.beginTransaction();
      ft.replace(R.id.content_frame, fragment);
      ft.commit();
    }

    in_drawer_list.setOnItemClickListener(new OnItemClickListener() {
      @Override public void onItemClick(
        final @Nullable AdapterView<?> parent,
        final @Nullable View view,
        final int position,
        final long id)
      {
        if (MainActivity.this.part_current != position) {
          MainActivity.this.part_current = position;

          final PartType part = NullCheck.notNull(in_parts.get(position));
          final Fragment fragment = part.getCurrentFragment();
          final FragmentTransaction ft = fm.beginTransaction();
          ft.replace(R.id.content_frame, fragment);
          ft.addToBackStack(null);
          ft.commit();
        }

        in_drawer.closeDrawer(GravityCompat.START);
      }
    });
  }
}
