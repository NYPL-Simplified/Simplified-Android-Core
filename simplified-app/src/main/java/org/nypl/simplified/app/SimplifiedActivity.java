package org.nypl.simplified.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nypl.simplified.app.catalog.BooksActivity;
import org.nypl.simplified.app.catalog.CatalogFeedActivity;
import org.nypl.simplified.app.catalog.CatalogFeedArgumentsLocalBooks;
import org.nypl.simplified.app.catalog.CatalogFeedArgumentsRemote;
import org.nypl.simplified.app.catalog.CatalogUpStackEntry;
import org.nypl.simplified.app.catalog.HoldsActivity;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.google.common.collect.ImmutableList;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The type of all activities in the app.
 */

@SuppressWarnings("boxing") public abstract class SimplifiedActivity extends
  Activity implements DrawerListener, OnItemClickListener
{
  private static int          ACTIVITY_COUNT;

  private static final Logger LOG;

  private static final String NAVIGATION_DRAWER_OPEN_ID;
  static {
    LOG = LogUtilities.getLog(SimplifiedActivity.class);
  }

  static {
    NAVIGATION_DRAWER_OPEN_ID =
      "org.nypl.simplified.app.SimplifiedActivity.drawer_open";
  }

  public static void setActivityArguments(
    final Bundle b,
    final boolean open_drawer)
  {
    NullCheck.notNull(b);
    b.putBoolean(SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID, open_drawer);
  }

  private @Nullable FrameLayout                             content_frame;
  private @Nullable DrawerLayout                            drawer;
  private @Nullable Map<String, FunctionType<Bundle, Unit>> drawer_arg_funcs;
  private @Nullable Map<String, Class<? extends Activity>>  drawer_classes_by_name;
  private @Nullable ArrayList<String>                       drawer_items;
  private @Nullable ListView                                drawer_list;
  private @Nullable Map<Class<? extends Activity>, String>  drawer_names_by_class;
  private boolean                                           finishing;
  private int                                               selected;

  private void finishWithConditionalAnimationOverride()
  {
    this.finish();

    /**
     * If this activity is the last activity, do not override the closing
     * transition animation.
     */

    if (SimplifiedActivity.ACTIVITY_COUNT > 1) {
      this.overridePendingTransition(0, 0);
    }
  }

  protected final FrameLayout getContentFrame()
  {
    return NullCheck.notNull(this.content_frame);
  }

  @Override public void onBackPressed()
  {
    SimplifiedActivity.LOG.debug("onBackPressed: {}", this);

    final DrawerLayout d = NullCheck.notNull(this.drawer);
    if (d.isDrawerOpen(GravityCompat.START)) {
      this.finishing = true;
      d.closeDrawer(GravityCompat.START);
    } else {

      /**
       * If this activity is the last activity, do not override the closing
       * transition animation.
       */

      this.finishWithConditionalAnimationOverride();
    }
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    SimplifiedActivity.LOG.debug("onCreate: {}", this);
    this.setContentView(R.layout.main);

    boolean open_drawer = true;

    final Intent i = NullCheck.notNull(this.getIntent());
    SimplifiedActivity.LOG.debug("non-null intent");
    final Bundle a = i.getExtras();
    if (a != null) {
      SimplifiedActivity.LOG.debug("non-null intent extras");
      open_drawer =
        a.getBoolean(SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID);
      SimplifiedActivity.LOG.debug("drawer requested: {}", open_drawer);
    }

    if (state != null) {
      SimplifiedActivity.LOG.debug("reinitializing");
      open_drawer =
        state.getBoolean(
          SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID,
          open_drawer);
    }

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final Resources rr = NullCheck.notNull(this.getResources());

    /**
     * Configure the navigation drawer.
     */

    final DrawerLayout d =
      NullCheck.notNull((DrawerLayout) this.findViewById(R.id.drawer_layout));
    final ListView dl =
      NullCheck.notNull((ListView) this.findViewById(R.id.left_drawer));
    final FrameLayout fl =
      NullCheck.notNull((FrameLayout) this.findViewById(R.id.content_frame));

    d.setDrawerListener(this);
    dl.setOnItemClickListener(this);

    final String app_name =
      NullCheck.notNull(rr.getString(R.string.app_name));
    final String catalog_name =
      NullCheck.notNull(rr.getString(R.string.catalog));
    final String holds_name = NullCheck.notNull(rr.getString(R.string.holds));
    final String books_name = NullCheck.notNull(rr.getString(R.string.books));
    final String settings_name =
      NullCheck.notNull(rr.getString(R.string.settings));

    final ArrayList<String> di = new ArrayList<String>();
    di.add(catalog_name);
    di.add(holds_name);
    di.add(books_name);
    di.add(settings_name);
    dl.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_item, di));

    /**
     * Set up a map of names ↔ classes.
     */

    final Map<String, Class<? extends Activity>> classes_by_name =
      new HashMap<String, Class<? extends Activity>>();
    classes_by_name.put(books_name, BooksActivity.class);
    classes_by_name.put(catalog_name, CatalogFeedActivity.class);
    classes_by_name.put(holds_name, HoldsActivity.class);
    classes_by_name.put(settings_name, SettingsActivity.class);

    final Map<Class<? extends Activity>, String> names_by_class =
      new HashMap<Class<? extends Activity>, String>();
    for (final Entry<String, Class<? extends Activity>> e : classes_by_name
      .entrySet()) {
      final Class<? extends Activity> c = NullCheck.notNull(e.getValue());
      final String n = NullCheck.notNull(e.getKey());
      assert names_by_class.containsKey(c) == false;
      names_by_class.put(c, n);
    }

    /**
     * Set up a map of part names to functions that configure argument
     * bundles.
     */

    final Map<String, FunctionType<Bundle, Unit>> da =
      new HashMap<String, FunctionType<Bundle, Unit>>();

    da.put(books_name, new FunctionType<Bundle, Unit>() {
      @Override public Unit call(
        final Bundle b)
      {
        final CatalogFeedArgumentsLocalBooks local =
          new CatalogFeedArgumentsLocalBooks(books_name);
        CatalogFeedActivity.setActivityArguments(b, local);
        return Unit.unit();
      }
    });

    da.put(catalog_name, new FunctionType<Bundle, Unit>() {
      @Override public Unit call(
        final Bundle b)
      {
        final ImmutableList<CatalogUpStackEntry> empty = ImmutableList.of();
        final CatalogFeedArgumentsRemote remote =
          new CatalogFeedArgumentsRemote(
            false,
            NullCheck.notNull(empty),
            app_name,
            app.getFeedInitialURI());
        CatalogFeedActivity.setActivityArguments(b, remote);
        return Unit.unit();
      }
    });

    da.put(holds_name, new FunctionType<Bundle, Unit>() {
      @Override public Unit call(
        final Bundle b)
      {
        SimplifiedActivity.setActivityArguments(b, false);
        return Unit.unit();
      }
    });

    da.put(settings_name, new FunctionType<Bundle, Unit>() {
      @Override public Unit call(
        final Bundle b)
      {
        SimplifiedActivity.setActivityArguments(b, false);
        return Unit.unit();
      }
    });

    /**
     * If the drawer should be open, open it.
     */

    if (open_drawer) {
      d.openDrawer(GravityCompat.START);
    }

    this.drawer_items = di;
    this.drawer_classes_by_name = classes_by_name;
    this.drawer_names_by_class = names_by_class;
    this.drawer_arg_funcs = da;
    this.drawer = d;
    this.drawer_list = dl;
    this.content_frame = fl;
    this.selected = -1;
    SimplifiedActivity.ACTIVITY_COUNT = SimplifiedActivity.ACTIVITY_COUNT + 1;
    SimplifiedActivity.LOG.debug(
      "activity count: {}",
      SimplifiedActivity.ACTIVITY_COUNT);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    SimplifiedActivity.LOG.debug("onDestroy: {}", this);
    SimplifiedActivity.ACTIVITY_COUNT = SimplifiedActivity.ACTIVITY_COUNT - 1;
  }

  @Override public final void onDrawerClosed(
    final @Nullable View drawerView)
  {
    SimplifiedActivity.LOG
      .debug("drawer closed, selected: {}", this.selected);

    /**
     * If the drawer is closing because the user pressed the back button, then
     * finish the activity.
     */

    if (this.finishing) {
      this.finishWithConditionalAnimationOverride();
      return;
    }

    /**
     * If the drawer is closing because the user selected an entry, start the
     * relevant activity.
     */

    if (this.selected != -1) {
      final ArrayList<String> di = NullCheck.notNull(this.drawer_items);
      final Map<String, Class<? extends Activity>> dc =
        NullCheck.notNull(this.drawer_classes_by_name);
      final Map<String, FunctionType<Bundle, Unit>> fas =
        NullCheck.notNull(this.drawer_arg_funcs);

      final String name = NullCheck.notNull(di.get(this.selected));
      final Class<? extends Activity> c = NullCheck.notNull(dc.get(name));
      final FunctionType<Bundle, Unit> fa = NullCheck.notNull(fas.get(name));

      final Bundle b = new Bundle();
      SimplifiedActivity.setActivityArguments(b, false);
      fa.call(b);

      final Intent i = new Intent();
      i.setClass(this, c);
      i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
      i.putExtras(b);
      this.startActivity(i);
    }

    this.selected = -1;
  }

  @Override public final void onDrawerOpened(
    final @Nullable View drawerView)
  {
    this.selected = -1;
  }

  @Override public final void onDrawerSlide(
    final @Nullable View drawerView,
    final float slideOffset)
  {
    // Nothing
  }

  @Override public final void onDrawerStateChanged(
    final int newState)
  {
    // Nothing
  }

  @Override public void onItemClick(
    final @Nullable AdapterView<?> parent,
    final @Nullable View view,
    final int position,
    final long id)
  {
    SimplifiedActivity.LOG.debug("selected navigation item: {}", position);

    final DrawerLayout d = NullCheck.notNull(this.drawer);
    d.closeDrawer(GravityCompat.START);
    this.selected = position;
  }

  @Override protected void onResume()
  {
    super.onResume();
    SimplifiedActivity.LOG.debug("onResume: {}", this);

    final Map<Class<? extends Activity>, String> dnbc =
      NullCheck.notNull(this.drawer_names_by_class);
    final List<String> di = NullCheck.notNull(this.drawer_items);

    final String name = NullCheck.notNull(dnbc.get(this.getClass()));
    SimplifiedActivity.LOG.debug("restored drawer name: {}", name);
    final int pos = di.indexOf(name);
    SimplifiedActivity.LOG.debug("restored selected item: {}", pos);

    final ListView dl = NullCheck.notNull(this.drawer_list);
    dl.setSelection(pos);
    dl.setItemChecked(pos, true);
  }

  @Override protected void onSaveInstanceState(
    final @Nullable Bundle state)
  {
    super.onSaveInstanceState(state);
    assert state != null;
    final DrawerLayout d = NullCheck.notNull(this.drawer);
    state.putBoolean(
      SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID,
      d.isDrawerOpen(GravityCompat.START));
  }
}
