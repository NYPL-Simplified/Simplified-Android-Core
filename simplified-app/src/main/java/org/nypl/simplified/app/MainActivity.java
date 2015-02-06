package org.nypl.simplified.app;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.widget.TabHost.TabSpec;

import com.io7m.jnull.Nullable;

public final class MainActivity extends FragmentActivity
{
  private @Nullable FragmentTabHost host;

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.main);

    final Resources rr = this.getResources();

    final FragmentTabHost h =
      (FragmentTabHost) this.findViewById(android.R.id.tabhost);
    h.setup(this, this.getSupportFragmentManager(), R.id.realcontent);

    final String books_name = rr.getString(R.string.books);
    final TabSpec books_tab = h.newTabSpec(books_name);
    books_tab.setIndicator(books_name);
    h.addTab(books_tab, BooksFragment.class, null);

    final String catalog_name = rr.getString(R.string.catalog);
    final TabSpec catalog_tab = h.newTabSpec(catalog_name);
    catalog_tab.setIndicator(catalog_name);
    h.addTab(catalog_tab, CatalogFragment.class, null);

    final String holds_name = rr.getString(R.string.holds);
    final TabSpec holds_tab = h.newTabSpec(holds_name);
    holds_tab.setIndicator(holds_name);
    h.addTab(holds_tab, HoldsFragment.class, null);

    final String settings_name = rr.getString(R.string.settings);
    final TabSpec settings_tab = h.newTabSpec(settings_name);
    settings_tab.setIndicator(settings_name);
    h.addTab(settings_tab, SettingsFragment.class, null);

    this.host = h;
  }
}
