package org.nypl.simplified.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.catalog.CatalogFeedActivity;
import org.nypl.simplified.app.catalog.CatalogFeedArgumentsLocalBooks;
import org.nypl.simplified.app.catalog.CatalogFeedArgumentsRemote;
import org.nypl.simplified.app.catalog.CatalogFeedArgumentsType;
import org.nypl.simplified.app.catalog.MainBooksActivity;
import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.app.catalog.MainHoldsActivity;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.books.core.BooksFeedSelection;
import org.nypl.simplified.books.core.FeedFacetPseudo;
import org.nypl.simplified.stack.ImmutableStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type of non-reader activities in the app.
 */

public abstract class SimplifiedActivity extends Activity
  implements DrawerListener, OnItemClickListener
{
  private static final Logger LOG;
  private static final String NAVIGATION_DRAWER_OPEN_ID;
  private static       int    ACTIVITY_COUNT;

  static {
    LOG = LogUtilities.getLog(SimplifiedActivity.class);
  }

  static {
    NAVIGATION_DRAWER_OPEN_ID =
      "org.nypl.simplified.app.SimplifiedActivity.drawer_open";
  }

  private @Nullable ArrayAdapter<SimplifiedPart> adapter;
  private @Nullable FrameLayout                  content_frame;
  private @Nullable DrawerLayout                 drawer;
  private @Nullable Map<SimplifiedPart, FunctionType<Bundle, Unit>>
                                                 drawer_arg_funcs;
  private @Nullable Map<SimplifiedPart, Class<? extends Activity>>
                                                 drawer_classes_by_name;
  private @Nullable List<SimplifiedPart>         drawer_items;
  private @Nullable ListView                     drawer_list;
  private @Nullable SharedPreferences            drawer_settings;
  private           boolean                      finishing;
  private           int                          selected;

  /**
   * Set the arguments for the activity that will be created.
   *
   * @param b           The argument bundle
   * @param open_drawer {@code true} iff the navigation drawer should be opened
   */

  public static void setActivityArguments(
    final Bundle b,
    final boolean open_drawer)
  {
    NullCheck.notNull(b);
    b.putBoolean(SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID, open_drawer);
  }

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

  private void hideKeyboard()
  {
    // Check if no view has focus:
    final View view = this.getCurrentFocus();
    if (view != null) {
      final InputMethodManager im = (InputMethodManager) this.getSystemService(
        Context.INPUT_METHOD_SERVICE);
      im.hideSoftInputFromWindow(
        view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
  }

  protected abstract SimplifiedPart navigationDrawerGetPart();

  protected final void navigationDrawerSetActionBarTitle()
  {
    final ActionBar bar = NullCheck.notNull(this.getActionBar());
    final Resources rr = NullCheck.notNull(this.getResources());
    final SimplifiedPart part = this.navigationDrawerGetPart();
    bar.setTitle(part.getPartName(rr));
  }

  protected abstract boolean navigationDrawerShouldShowIndicator();

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
      open_drawer = a.getBoolean(SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID);
      SimplifiedActivity.LOG.debug("drawer requested: {}", open_drawer);
    }

    /**
     * The activity is being re-initialized. Set the drawer to whatever
     * state it was in when the activity was destroyed.
     */

    if (state != null) {
      SimplifiedActivity.LOG.debug("reinitializing");
      open_drawer = state.getBoolean(
        SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID, open_drawer);
    }

    /**
     * As per the Android design documents: If the user has manually opened
     * the navigation drawer, then the user is assumed to understand how the
     * drawer works. Therefore, if it appears that the drawer should be
     * opened, check to see if it should actually be closed.
     */

    final SharedPreferences in_drawer_settings =
      NullCheck.notNull(this.getSharedPreferences("drawer-settings", 0));
    if (in_drawer_settings.getBoolean("has-opened-manually", false)) {
      SimplifiedActivity.LOG.debug(
        "user has manually opened drawer in the past, not opening it now!");
      open_drawer = false;
    }
    this.drawer_settings = in_drawer_settings;

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
    d.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    dl.setOnItemClickListener(this);

    final String app_name = NullCheck.notNull(rr.getString(R.string.app_name));

    final List<SimplifiedPart> di = new ArrayList<SimplifiedPart>();
    di.add(SimplifiedPart.PART_CATALOG);
    di.add(SimplifiedPart.PART_BOOKS);
    di.add(SimplifiedPart.PART_HOLDS);
    di.add(SimplifiedPart.PART_SETTINGS);

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());
    this.adapter =
      new ArrayAdapter<SimplifiedPart>(this, R.layout.drawer_item, di)
      {
        @Override public View getView(
          final int position,
          final @Nullable View reuse,
          final @Nullable ViewGroup parent)
        {
          final View v;
          if (reuse != null) {
            v = reuse;
          } else {
            v = inflater.inflate(R.layout.drawer_item, parent, false);
          }

          final SimplifiedPart part = NullCheck.notNull(di.get(position));
          final TextView tv =
            NullCheck.notNull((TextView) v.findViewById(android.R.id.text1));
          tv.setText(part.getPartName(rr));
          return v;
        }
      };

    dl.setAdapter(this.adapter);

    /**
     * Set up a map of names ↔ classes.
     */

    final Map<SimplifiedPart, Class<? extends Activity>> classes_by_name =
      new HashMap<SimplifiedPart, Class<? extends Activity>>();
    classes_by_name.put(SimplifiedPart.PART_BOOKS, MainBooksActivity.class);
    classes_by_name.put(
      SimplifiedPart.PART_CATALOG, MainCatalogActivity.class);
    classes_by_name.put(SimplifiedPart.PART_HOLDS, MainHoldsActivity.class);
    classes_by_name.put(SimplifiedPart.PART_SETTINGS, MainSettingsActivity.class);

    /**
     * Set up a map of part names to functions that configure argument
     * bundles.
     */

    final Map<SimplifiedPart, FunctionType<Bundle, Unit>> da =
      new HashMap<SimplifiedPart, FunctionType<Bundle, Unit>>();

    da.put(
      SimplifiedPart.PART_BOOKS, new FunctionType<Bundle, Unit>()
      {
        @Override public Unit call(
          final Bundle b)
        {
          final OptionType<String> no_search = Option.none();
          final ImmutableStack<CatalogFeedArgumentsType> empty_stack =
            ImmutableStack.empty();
          final CatalogFeedArgumentsLocalBooks local =
            new CatalogFeedArgumentsLocalBooks(
              empty_stack,
              SimplifiedPart.PART_BOOKS.getPartName(rr),
              FeedFacetPseudo.FacetType.SORT_BY_TITLE,
              no_search,
              BooksFeedSelection.BOOKS_FEED_LOANED);
          CatalogFeedActivity.setActivityArguments(b, local);
          return Unit.unit();
        }
      });

    da.put(
      SimplifiedPart.PART_CATALOG, new FunctionType<Bundle, Unit>()
      {
        @Override public Unit call(
          final Bundle b)
        {
          final ImmutableStack<CatalogFeedArgumentsType> empty =
            ImmutableStack.empty();
          final CatalogFeedArgumentsRemote remote =
            new CatalogFeedArgumentsRemote(
              false,
              NullCheck.notNull(empty),
              app_name,
              app.getFeedInitialURI(),
              false);
          CatalogFeedActivity.setActivityArguments(b, remote);
          return Unit.unit();
        }
      });

    da.put(
      SimplifiedPart.PART_HOLDS, new FunctionType<Bundle, Unit>()
      {
        @Override public Unit call(
          final Bundle b)
        {
          final OptionType<String> no_search = Option.none();
          final ImmutableStack<CatalogFeedArgumentsType> empty_stack =
            ImmutableStack.empty();
          final CatalogFeedArgumentsLocalBooks local =
            new CatalogFeedArgumentsLocalBooks(
              empty_stack,
              SimplifiedPart.PART_HOLDS.getPartName(rr),
              FeedFacetPseudo.FacetType.SORT_BY_TITLE,
              no_search,
              BooksFeedSelection.BOOKS_FEED_HOLDS);
          CatalogFeedActivity.setActivityArguments(b, local);
          return Unit.unit();
        }
      });

    da.put(
      SimplifiedPart.PART_SETTINGS, new FunctionType<Bundle, Unit>()
      {
        @Override public Unit call(
          final Bundle b)
        {
          SimplifiedActivity.setActivityArguments(b, false);
          return Unit.unit();
        }
      });

    if (this.navigationDrawerShouldShowIndicator()) {
      SimplifiedActivity.LOG.debug("setting navigation drawer indicator");
      final ActionBar bar = this.getActionBar();
      bar.setHomeAsUpIndicator(R.drawable.ic_drawer);
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(true);
    }

    /**
     * If the drawer should be open, open it.
     */

    if (open_drawer) {
      d.openDrawer(GravityCompat.START);
    }

    this.drawer_items = di;
    this.drawer_classes_by_name = classes_by_name;
    this.drawer_arg_funcs = da;
    this.drawer = d;
    this.drawer_list = dl;
    this.content_frame = fl;
    this.selected = -1;
    SimplifiedActivity.ACTIVITY_COUNT = SimplifiedActivity.ACTIVITY_COUNT + 1;
    SimplifiedActivity.LOG.debug(
      "activity count: {}", SimplifiedActivity.ACTIVITY_COUNT);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    SimplifiedActivity.LOG.debug("onDestroy: {}", this);
    SimplifiedActivity.ACTIVITY_COUNT = SimplifiedActivity.ACTIVITY_COUNT - 1;
  }

  @Override public final void onDrawerClosed(
    final @Nullable View drawer_view)
  {
    SimplifiedActivity.LOG.debug(
      "onDrawerClosed: selected: {}", this.selected);

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
      final List<SimplifiedPart> di = NullCheck.notNull(this.drawer_items);
      final Map<SimplifiedPart, Class<? extends Activity>> dc =
        NullCheck.notNull(this.drawer_classes_by_name);
      final Map<SimplifiedPart, FunctionType<Bundle, Unit>> fas =
        NullCheck.notNull(this.drawer_arg_funcs);

      final SimplifiedPart name = NullCheck.notNull(di.get(this.selected));
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
    final @Nullable View drawer_view)
  {
    this.selected = -1;
    SimplifiedActivity.LOG.debug("onDrawerOpened: {}", drawer_view);

    final SharedPreferences in_drawer_settings =
      NullCheck.notNull(this.drawer_settings);
    in_drawer_settings.edit().putBoolean("has-opened-manually", true).apply();

    this.hideKeyboard();
  }

  @Override public final void onDrawerSlide(
    final @Nullable View drawer_view,
    final float slide_offset)
  {
    // Nothing
  }

  @Override public final void onDrawerStateChanged(
    final int new_state)
  {
    SimplifiedActivity.LOG.debug("onDrawerStateChanged: {}", new_state);
  }

  @Override public void onItemClick(
    final @Nullable AdapterView<?> parent,
    final @Nullable View view,
    final int position,
    final long id)
  {
    SimplifiedActivity.LOG.debug("onItemClick: {}", position);

    final DrawerLayout d = NullCheck.notNull(this.drawer);
    d.closeDrawer(GravityCompat.START);
    this.selected = position;
  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn)
  {
    final MenuItem item = NullCheck.notNull(item_mn);
    switch (item.getItemId()) {

      case android.R.id.home: {
        final DrawerLayout d = NullCheck.notNull(this.drawer);
        if (d.isDrawerOpen(GravityCompat.START)) {
          d.closeDrawer(GravityCompat.START);
        } else {
          d.openDrawer(GravityCompat.START);
        }

        return super.onOptionsItemSelected(item);
      }

      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }

  @Override protected void onResume()
  {
    super.onResume();
    SimplifiedActivity.LOG.debug("onResume: {}", this);

    final List<SimplifiedPart> di = NullCheck.notNull(this.drawer_items);
    final ListView dl = NullCheck.notNull(this.drawer_list);
    final SimplifiedPart p = this.navigationDrawerGetPart();

    final int pos = di.indexOf(p);
    SimplifiedActivity.LOG.debug("restored selected item: {}", pos);

    dl.setSelection(pos);
    dl.setItemChecked(pos, true);
  }

  @Override protected void onSaveInstanceState(
    final @Nullable Bundle state)
  {
    super.onSaveInstanceState(state);
    final Bundle state_nn = NullCheck.notNull(state);
    final DrawerLayout d = NullCheck.notNull(this.drawer);
    state_nn.putBoolean(
      SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID,
      d.isDrawerOpen(GravityCompat.START));
  }
}
