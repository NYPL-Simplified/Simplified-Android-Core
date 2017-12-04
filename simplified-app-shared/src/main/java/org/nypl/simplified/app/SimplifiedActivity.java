package org.nypl.simplified.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.nypl.simplified.app.catalog.CatalogFeedActivity;
import org.nypl.simplified.app.catalog.CatalogFeedArgumentsLocalBooks;
import org.nypl.simplified.app.catalog.CatalogFeedArgumentsRemote;
import org.nypl.simplified.app.catalog.CatalogFeedArgumentsType;
import org.nypl.simplified.app.catalog.MainBooksActivity;
import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.app.catalog.MainHoldsActivity;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.BooksControllerConfigurationType;
import org.nypl.simplified.books.core.BooksFeedSelection;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedFacetPseudo;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.multilibrary.AccountsRegistry;
import org.nypl.simplified.prefs.Prefs;
import org.nypl.simplified.stack.ImmutableStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type of non-reader activities in the app.
 *
 * This is the where navigation drawer configuration takes place.
 */

public abstract class SimplifiedActivity extends Activity
  implements DrawerListener, OnItemClickListener
{
  private static final Logger  LOG;
  private static final String  NAVIGATION_DRAWER_OPEN_ID;
  private static       int     ACTIVITY_COUNT;
  private static       boolean DEVICE_ACTIVATED;

  static {
    LOG = LogUtilities.getLog(SimplifiedActivity.class);
  }

  static {
    NAVIGATION_DRAWER_OPEN_ID =
      "org.nypl.simplified.app.SimplifiedActivity.drawer_open";
  }

  private @Nullable ArrayAdapter<SimplifiedPart> adapter;
  private @Nullable ArrayAdapter<Object>         adapter_accounts;
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
  private           SimplifiedCatalogAppServicesType app;
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
    final Resources rr = NullCheck.notNull(this.getResources());

    final DrawerLayout d = NullCheck.notNull(this.drawer);
    final ActionBar bar = this.getActionBar();
    if (d.isDrawerOpen(GravityCompat.START)) {
      this.finishing = true;
      d.closeDrawer(GravityCompat.START);
      bar.setHomeActionContentDescription(rr.getString(R.string.navigation_accessibility_drawer_show));
    } else {

      /**
       * If this activity is the last activity, do not override the closing
       * transition animation.
       */


      if (SimplifiedActivity.ACTIVITY_COUNT == 1)
      {

        if (this.getClass() != MainCatalogActivity.class) {
          // got to main catalog activity
          //final DrawerLayout d = NullCheck.notNull(this.drawer);
          this.selected = 1;
          this.startSideBarActivity();


        } else {
          d.openDrawer(GravityCompat.START);
          bar.setHomeActionContentDescription(rr.getString(R.string.navigation_accessibility_drawer_hide));
        }
      } else {
        this.finishWithConditionalAnimationOverride();
      }
    }
  }

  private void startSideBarActivity() {
    if (this.selected != -1) {
      final List<SimplifiedPart> di = NullCheck.notNull(this.drawer_items);

      final SimplifiedPart name = NullCheck.notNull(di.get(this.selected));

      if (this.selected > 0) {
        final Map<SimplifiedPart, Class<? extends Activity>> dc =
          NullCheck.notNull(this.drawer_classes_by_name);
        final Class<? extends Activity> c = NullCheck.notNull(dc.get(name));

        final Map<SimplifiedPart, FunctionType<Bundle, Unit>> fas =
          NullCheck.notNull(this.drawer_arg_funcs);
        final FunctionType<Bundle, Unit> fa = NullCheck.notNull(fas.get(name));

        final Bundle b = new Bundle();
        SimplifiedActivity.setActivityArguments(b, false);
        fa.call(b);

        final Intent i = new Intent();
        i.setClass(this, c);
        i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        i.putExtras(b);
        this.startActivity(i);

        this.overridePendingTransition(0, 0);
      } else {
        // replace drawer with selection of libraries
        final ListView dl =
          NullCheck.notNull((ListView) this.findViewById(R.id.left_drawer));

        dl.setOnItemClickListener(this);
        dl.setAdapter(this.adapter_accounts);

      }
    }

    if (this.selected > 0) {
      this.selected = -1;
      final DrawerLayout d = NullCheck.notNull(this.drawer);
      d.closeDrawer(GravityCompat.START);
    }
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {

    final int id = Simplified.getCurrentAccount().getId();
    if (id == 0) {
      setTheme(R.style.SimplifiedTheme_NYPL);
    }
    else if (id == 1) {
      setTheme(R.style.SimplifiedTheme_BPL);
    }
    else if (id == 7) {
      setTheme(R.style.SimplifiedTheme_ACL);
    }
    else if (id == 8) {
      setTheme(R.style.SimplifiedTheme_HCLS);
    }
    else if (id == 9) {
      setTheme(R.style.SimplifiedTheme_MCPL);
    }
    else if (id == 10) {
      setTheme(R.style.SimplifiedTheme_FCPL);
    }
    else if (id == 11) {
      setTheme(R.style.SimplifiedTheme_AACPL);
    }
    else if (id == 12) {
      setTheme(R.style.SimplifiedTheme_BGC);
    }
    else if (id == 13) {
      setTheme(R.style.SimplifiedTheme_SMCL);
    }
    else if (id == 14) {
      setTheme(R.style.SimplifiedTheme_CL);
    }
    else if (id == 15) {
      setTheme(R.style.SimplifiedTheme_CCPL);
    }
    else if (id == 16) {
      setTheme(R.style.SimplifiedTheme_CCL);
    }
    else if (id == 17) {
      setTheme(R.style.SimplifiedTheme_BCL);
    }
    else if (id == 18) {
      setTheme(R.style.SimplifiedTheme_LAPL);
    }
    else if (id == 19) {
      setTheme(R.style.SimplifiedTheme_PCL);
    }
    else if (id == 20) {
      setTheme(R.style.SimplifiedTheme_SCCL);
    }
    else if (id == 21) {
      setTheme(R.style.SimplifiedTheme_ACLS);
    }
    else if (id == 22) {
      setTheme(R.style.SimplifiedTheme_REL);
    }
    else if (id == 23) {
      setTheme(R.style.SimplifiedTheme_WCFL);
    }
    else {
      setTheme(R.style.SimplifiedTheme);
    }

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

    /**
     * Holds are an optional feature. If they are disabled, then the item
     * is simply removed from the navigation drawer.
     */

    this.app =
      Simplified.getCatalogAppServices();
    final Resources rr = NullCheck.notNull(this.getResources());
    final boolean holds_enabled = rr.getBoolean(R.bool.feature_holds_enabled);

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
    d.setDrawerTitle(Gravity.LEFT, rr.getString(R.string.navigation_accessibility));

    final String app_name = NullCheck.notNull(rr.getString(R.string.feature_app_name));
    final List<SimplifiedPart> di = new ArrayList<SimplifiedPart>();
    di.add(SimplifiedPart.PART_SWITCHER);

    di.add(SimplifiedPart.PART_CATALOG);
    di.add(SimplifiedPart.PART_BOOKS);
    if (holds_enabled && Simplified.getCurrentAccount().supportsReservations()) {
      di.add(SimplifiedPart.PART_HOLDS);
    }
    di.add(SimplifiedPart.PART_SETTINGS);


    final List<Object> dia = new ArrayList<Object>();

    final JSONArray registry = new AccountsRegistry(this).getCurrentAccounts(Simplified.getSharedPrefs());
    for (int index = 0; index < registry.length(); ++index) {
      try {
        dia.add(new Account(registry.getJSONObject(index)));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    dia.add(SimplifiedPart.PART_MANAGE_ACCOUNTS);

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());

    this.adapter_accounts =
      new ArrayAdapter<Object>(this,  R.layout.drawer_item_account, dia)
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
            v = inflater.inflate(R.layout.drawer_item_account, parent, false);
          }

          final Object object = NullCheck.notNull(dia.get(position));

          if (object instanceof Account) {

            final Account account = (Account) object;
            final TextView tv =
              NullCheck.notNull((TextView) v.findViewById(android.R.id.text1));
            tv.setText(account.getName());

            final ImageView icon_view =
              NullCheck.notNull((ImageView) v.findViewById(R.id.cellIcon));
            if (account.getId() == 0) {
              icon_view.setImageResource(R.drawable.account_logo_nypl);
            } else if (account.getId() == 1) {
              icon_view.setImageResource(R.drawable.account_logo_bpl);
            } else if (account.getId() == 2) {
              icon_view.setImageResource(R.drawable.account_logo_instant);
            } else if (account.getId() == 7) {
              icon_view.setImageResource(R.drawable.account_logo_alameda);
            } else if (account.getId() == 8) {
              icon_view.setImageResource(R.drawable.account_logo_hcls);
            } else if (account.getId() == 9) {
              icon_view.setImageResource(R.drawable.account_logo_mcpl);
            } else if (account.getId() == 10) {
              icon_view.setImageResource(R.drawable.account_logo_fcpl);
            } else if (account.getId() == 11) {
              icon_view.setImageResource(R.drawable.account_logo_anne_arundel);
            } else if (account.getId() == 12) {
              icon_view.setImageResource(R.drawable.account_logo_bgc);
            } else if (account.getId() == 13) {
              icon_view.setImageResource(R.drawable.account_logo_smcl);
            } else if (account.getId() == 14) {
              icon_view.setImageResource(R.drawable.account_logo_cl);
            } else if (account.getId() == 15) {
              icon_view.setImageResource(R.drawable.account_logo_ccpl);
            } else if (account.getId() == 16) {
              icon_view.setImageResource(R.drawable.account_logo_ccl);
            } else if (account.getId() == 17) {
              icon_view.setImageResource(R.drawable.account_logo_bcl);
            } else if (account.getId() == 18) {
              icon_view.setImageResource(R.drawable.account_logo_lapl);
            } else if (account.getId() == 19) {
              icon_view.setImageResource(R.drawable.account_logo_pcl);
            } else if (account.getId() == 20) {
              icon_view.setImageResource(R.drawable.account_logo_sccl);
            } else if (account.getId() == 21) {
              icon_view.setImageResource(R.drawable.account_logo_acls);
            } else if (account.getId() == 22) {
              icon_view.setImageResource(R.drawable.account_logo_rel);
            } else if (account.getId() == 23) {
              icon_view.setImageResource(R.drawable.account_logo_wcfl);
            }
          } else {
            final ImageView icon_view =
              NullCheck.notNull((ImageView) v.findViewById(R.id.cellIcon));
            icon_view.setImageResource(R.drawable.menu_icon_settings);
            final TextView tv =
              NullCheck.notNull((TextView) v.findViewById(android.R.id.text1));
            tv.setText(R.string.settings_manage_accounts);

          }
          return v;
        }
      };


    this.adapter =
      new ArrayAdapter<SimplifiedPart>(this, R.layout.drawer_item, di)
      {
        @Override public View getView(
          final int position,
          final @Nullable View reuse,
          final @Nullable ViewGroup parent)
        {
          View v;
          if (reuse != null) {
            v = reuse;
          } else {
            v = inflater.inflate(R.layout.drawer_item, parent, false);
          }
          final SimplifiedPart part = NullCheck.notNull(di.get(position));

          if (part.equals(SimplifiedPart.PART_SWITCHER)) {
            v = inflater.inflate(R.layout.drawer_item_current_account, parent, false);
          }

          final TextView tv =
            NullCheck.notNull((TextView) v.findViewById(android.R.id.text1));

          final ImageView icon_view =
            NullCheck.notNull((ImageView) v.findViewById(R.id.cellIcon));


          if (part.equals(SimplifiedPart.PART_SWITCHER)) {
            v.setBackgroundResource(R.drawable.textview_underline);
            final Prefs prefs = Simplified.getSharedPrefs();
            final Account account = new AccountsRegistry(SimplifiedActivity.this).getAccount(prefs.getInt("current_account"));
            tv.setText(account.getName());
            tv.setTextColor(Color.parseColor(Simplified.getCurrentAccount().getMainColor()));

            if (account.getId() == 0) {
              icon_view.setImageResource(R.drawable.account_logo_nypl);
            } else if (account.getId() == 1) {
              icon_view.setImageResource(R.drawable.account_logo_bpl);
            } else if (account.getId() == 2) {
              icon_view.setImageResource(R.drawable.account_logo_instant);
            } else if (account.getId() == 7) {
              icon_view.setImageResource(R.drawable.account_logo_alameda);
            } else if (account.getId() == 8) {
              icon_view.setImageResource(R.drawable.account_logo_hcls);
            } else if (account.getId() == 9) {
              icon_view.setImageResource(R.drawable.account_logo_mcpl);
            } else if (account.getId() == 10) {
              icon_view.setImageResource(R.drawable.account_logo_fcpl);
            } else if (account.getId() == 11) {
              icon_view.setImageResource(R.drawable.account_logo_anne_arundel);
            } else if (account.getId() == 12) {
              icon_view.setImageResource(R.drawable.account_logo_bgc);
            } else if (account.getId() == 13) {
              icon_view.setImageResource(R.drawable.account_logo_smcl);
            } else if (account.getId() == 14) {
              icon_view.setImageResource(R.drawable.account_logo_cl);
            } else if (account.getId() == 15) {
              icon_view.setImageResource(R.drawable.account_logo_ccpl);
            } else if (account.getId() == 16) {
              icon_view.setImageResource(R.drawable.account_logo_ccl);
            } else if (account.getId() == 17) {
              icon_view.setImageResource(R.drawable.account_logo_bcl);
            } else if (account.getId() == 18) {
              icon_view.setImageResource(R.drawable.account_logo_lapl);
            } else if (account.getId() == 19) {
              icon_view.setImageResource(R.drawable.account_logo_pcl);
            } else if (account.getId() == 20) {
              icon_view.setImageResource(R.drawable.account_logo_sccl);
            } else if (account.getId() == 21) {
              icon_view.setImageResource(R.drawable.account_logo_acls);
            } else if (account.getId() == 22) {
              icon_view.setImageResource(R.drawable.account_logo_rel);
            } else if (account.getId() == 23) {
              icon_view.setImageResource(R.drawable.account_logo_wcfl);
            }

          } else {
            tv.setText(part.getPartName(rr));
            if (dl.getCheckedItemPosition() == position) {
              tv.setContentDescription(tv.getText() + ". selected.");
              if (SimplifiedPart.PART_CATALOG == part) {
                icon_view.setImageResource(R.drawable.menu_icon_catalog_white);
              } else if (SimplifiedPart.PART_BOOKS == part) {
                icon_view.setImageResource(R.drawable.menu_icon_books_white);
              } else if (SimplifiedPart.PART_HOLDS == part) {
                icon_view.setImageResource(R.drawable.menu_icon_holds_white);
              } else if (SimplifiedPart.PART_SETTINGS == part) {
                icon_view.setImageResource(R.drawable.menu_icon_settings_white);
              }

            }
            else
            {
              if (SimplifiedPart.PART_CATALOG == part) {
                icon_view.setImageResource(R.drawable.menu_icon_catalog);
              } else if (SimplifiedPart.PART_BOOKS == part) {
                icon_view.setImageResource(R.drawable.menu_icon_books);
              } else if (SimplifiedPart.PART_HOLDS == part) {
                icon_view.setImageResource(R.drawable.menu_icon_holds);
              } else if (SimplifiedPart.PART_SETTINGS == part) {
                icon_view.setImageResource(R.drawable.menu_icon_settings);
              }

            }
          }

          return v;
        }
      };

    dl.setAdapter(this.adapter);

    /**
     * Set up a map of names ↔ classes. This is used to start an activity
     * by class, given a {@link SimplifiedPart}.
     */

    final Map<SimplifiedPart, Class<? extends Activity>> classes_by_name =
      new HashMap<SimplifiedPart, Class<? extends Activity>>();
    classes_by_name.put(SimplifiedPart.PART_BOOKS, MainBooksActivity.class);
    classes_by_name.put(
      SimplifiedPart.PART_CATALOG, MainCatalogActivity.class);
    if (holds_enabled && Simplified.getCurrentAccount().supportsReservations()) {
      classes_by_name.put(SimplifiedPart.PART_HOLDS, MainHoldsActivity.class);
    }
    classes_by_name.put(
      SimplifiedPart.PART_SETTINGS, MainSettingsActivity.class);

    /**
     * Set up a map of part names to functions that configure argument
     * bundles. Given a {@link SimplifiedPart}, this allows the construction
     * of an argument bundle for the target activity class.
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
          final BooksType books = SimplifiedActivity.this.app.getBooks();
          final BooksControllerConfigurationType config =
            books.booksGetConfiguration();

          final ImmutableStack<CatalogFeedArgumentsType> empty =
            ImmutableStack.empty();
          final CatalogFeedArgumentsRemote remote =
            new CatalogFeedArgumentsRemote(
              false,
              NullCheck.notNull(empty),
              app_name,
              config.getCurrentRootFeedURI(),
              false);
          CatalogFeedActivity.setActivityArguments(b, remote);
          return Unit.unit();
        }
      });

    if (holds_enabled && Simplified.getCurrentAccount().supportsReservations()) {
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
    }

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
    da.put(
      SimplifiedPart.PART_SWITCHER, new FunctionType<Bundle, Unit>()
      {
        @Override public Unit call(
          final Bundle b)
        {
          SimplifiedActivity.setActivityArguments(b, false);
          return Unit.unit();
        }
      });

    /**
     * Show or hide the three dashes next to the home button.
     */

    final ActionBar bar = this.getActionBar();
    if (this.navigationDrawerShouldShowIndicator()) {
      SimplifiedActivity.LOG.debug("setting navigation drawer indicator");
      if (android.os.Build.VERSION.SDK_INT < 21) {
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setHomeButtonEnabled(true);
      }
    }

    /**
     * If the drawer should be open, open it.
     */

    if (open_drawer) {
      d.openDrawer(GravityCompat.START);
      bar.setHomeActionContentDescription(rr.getString(R.string.navigation_accessibility_drawer_hide));
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


//    UIThread.runOnUIThreadDelayed(
//      new Runnable() {
//        @Override
//        public void run() {
//
//          if (!SimplifiedActivity.DEVICE_ACTIVATED && Simplified.getCurrentAccount().needsAuth()) {
//            // Don't try to activate the device unless we're connected to the Internet, since
//            // it will discard its credentials if it fails.
//            final ConnectivityManager connectivity_manager = (ConnectivityManager) SimplifiedActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
//            final NetworkInfo network_info = connectivity_manager.getActiveNetworkInfo();
//            if (network_info != null && network_info.isConnected()) {
//              // This is commented out because it turns out that activating the device on startup breaks
//              // decryption until a book is fulfilled.
//              SimplifiedActivity.DEVICE_ACTIVATED = true;
//            }
//          }
//
//        }
//      }, 3000L);


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
    final ListView dl =
      NullCheck.notNull((ListView) this.findViewById(R.id.left_drawer));

    if (dl.getAdapter().equals(this.adapter)) {

      SimplifiedActivity.LOG.debug("onItemClick: {}", position);
      final Resources rr = NullCheck.notNull(this.getResources());

      final ActionBar bar = this.getActionBar();
      bar.setHomeActionContentDescription(rr.getString(R.string.navigation_accessibility_drawer_show));
      this.selected = position;
      this.startSideBarActivity();
    } else {
      // select library

      final Object object = this.adapter_accounts.getItem(position);

      if (object instanceof Account) {
        final Account account = (Account) object;


        if (account.getId() != Simplified.getCurrentAccount().getId()) {

          final Prefs prefs = Simplified.getSharedPrefs();
          prefs.putInt("current_account", account.getId());

          dl.setAdapter(this.adapter);

          this.app =
            Simplified.getCatalogAppServices();

          UIThread.runOnUIThreadDelayed(
            new Runnable() {
              @Override
              public void run() {

                final Resources rr = NullCheck.notNull(SimplifiedActivity.this.getResources());
                final ActionBar bar = SimplifiedActivity.this.getActionBar();
                bar.setHomeActionContentDescription(rr.getString(R.string.navigation_accessibility_drawer_show));
                SimplifiedActivity.this.selected = 1;
                SimplifiedActivity.this.startSideBarActivity();


                if (Simplified.getSharedPrefs().contains("destroy_database") && Simplified.getSharedPrefs().getInt("destroy_database") == Simplified.getCurrentAccount().getId())
                {
                    SimplifiedActivity.this.app.destroyDatabase();
                    Simplified.getSharedPrefs().remove("destroy_database");
                }

              }
            }, 30L);
        } else {
          dl.setAdapter(this.adapter);
        }
      } else {
        this.selected = this.adapter.getCount() - 1;
        this.startSideBarActivity();
      }
    }

  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn)
  {
    final MenuItem item = NullCheck.notNull(item_mn);
    final Resources rr = NullCheck.notNull(this.getResources());

    switch (item.getItemId()) {

      case android.R.id.home: {
        final DrawerLayout d = NullCheck.notNull(this.drawer);
        final ActionBar bar = this.getActionBar();
        if (d.isDrawerOpen(GravityCompat.START)) {
          d.closeDrawer(GravityCompat.START);
          bar.setHomeActionContentDescription(rr.getString(R.string.navigation_accessibility_drawer_show));
        } else {
          d.openDrawer(GravityCompat.START);
          bar.setHomeActionContentDescription(rr.getString(R.string.navigation_accessibility_drawer_hide));
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

    // opening the drawer, when the user comes back from the help section, the view will not be empty.

  }

  @Override protected void onSaveInstanceState(
    final @Nullable Bundle state)
  {
    super.onSaveInstanceState(state);

    /**
     * Save the state of the navigation drawer. The intention here is that
     * the draw will correctly be open or closed based on the state it was
     * in when the user left the activity. In practice, the drawer tends to
     * close whenever the user moves to a new activity, so saving and restoring
     * may be redundant.
     */

    final Bundle state_nn = NullCheck.notNull(state);
    final DrawerLayout d = NullCheck.notNull(this.drawer);
    state_nn.putBoolean(
      SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID,
      d.isDrawerOpen(GravityCompat.START));
  }
}
