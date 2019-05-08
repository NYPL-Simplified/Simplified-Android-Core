package org.nypl.simplified.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
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

import com.google.common.collect.ImmutableList;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.accounts.api.AccountProvider;
import org.nypl.simplified.app.catalog.CatalogFeedActivity;
import org.nypl.simplified.app.catalog.CatalogFeedArguments;
import org.nypl.simplified.app.catalog.MainBooksActivity;
import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.app.catalog.MainHoldsActivity;
import org.nypl.simplified.app.images.ImageAccountIcons;
import org.nypl.simplified.app.profiles.ProfileSelectionActivity;
import org.nypl.simplified.app.profiles.ProfileSwitchDialog;
import org.nypl.simplified.app.profiles.ProfileTimeOutActivity;
import org.nypl.simplified.app.settings.SettingsActivity;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.feeds.api.FeedBooksSelection;
import org.nypl.simplified.observable.ObservableSubscriptionType;
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent;
import org.nypl.simplified.profiles.api.ProfileEvent;
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException;
import org.nypl.simplified.profiles.api.ProfileNonexistentAccountProviderException;
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType;
import org.nypl.simplified.stack.ImmutableStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.FacetType.SORT_BY_TITLE;
import static org.nypl.simplified.profiles.api.ProfilesDatabaseType.AnonymousProfileEnabled;

/**
 * The type of activities that have a navigation drawer.
 */

public abstract class NavigationDrawerActivity extends ProfileTimeOutActivity
  implements DrawerListener, OnItemClickListener {

  private static final Logger LOG;
  private static final String NAVIGATION_DRAWER_OPEN_ID;

  static {
    LOG = LoggerFactory.getLogger(NavigationDrawerActivity.class);
    NAVIGATION_DRAWER_OPEN_ID = "org.nypl.simplified.app.NavigationDrawerActivity.drawer_open";
  }

  private FrameLayout content_frame;
  private List<NavigationDrawerItemType> drawer_items_initial;
  private NavigationDrawerArrayAdapter drawer_adapter;
  private ListView drawer_list_view;
  private SharedPreferences drawer_settings;
  private boolean finishing;
  private ObservableSubscriptionType<ProfileEvent> profile_event_subscription;
  private DrawerLayout drawer_layout;
  private boolean open_drawer;

  /**
   * Set the arguments for the activity that will be created.
   *
   * @param b           The argument bundle
   * @param open_drawer {@code true} iff the navigation drawer should be opened
   */

  public static void setActivityArguments(
    final Bundle b,
    final boolean open_drawer) {
    NullCheck.notNull(b);
    b.putBoolean(NAVIGATION_DRAWER_OPEN_ID, open_drawer);
  }

  private static ImmutableList<NavigationDrawerItemType> calculateDrawerItemsInitial(
    final Activity activity) {

    final ImmutableList.Builder<NavigationDrawerItemType> drawer_items = ImmutableList.builder();
    drawer_items.add(new NavigationDrawerItemAccountCurrent(activity));
    drawer_items.add(new NavigationDrawerItemCatalog(activity));
    drawer_items.add(new NavigationDrawerItemBooks(activity));
    if (activity.getResources().getBoolean(R.bool.feature_holds_enabled)) {
      drawer_items.add(new NavigationDrawerItemHolds(activity));
    }
    drawer_items.add(new NavigationDrawerItemSettings(activity));

    final ProfilesControllerType profiles = Simplified.getProfilesController();
    if (profiles.profileAnonymousEnabled() == AnonymousProfileEnabled.ANONYMOUS_PROFILE_DISABLED) {
      drawer_items.add(new NavigationDrawerItemSwitchProfile(activity));
    }
    return drawer_items.build();
  }

  private static ImmutableList<NavigationDrawerItemType> calculateDrawerItemsAccounts(
    final Activity activity) {

    try {
      UIThread.checkIsUIThread();

      final ImmutableList.Builder<NavigationDrawerItemType> drawer_items =
        ImmutableList.builder();
      final ImmutableList<AccountProvider> drawer_item_accounts =
        Simplified.getProfilesController().profileCurrentlyUsedAccountProviders();

      for (final AccountProvider account : drawer_item_accounts) {
        drawer_items.add(new NavigationDrawerItemAccountSelectSpecific(activity, account));
      }

      drawer_items.add(new NavigationDrawerItemAccountManage(activity));
      return drawer_items.build();
    } catch (final ProfileNoneCurrentException | ProfileNonexistentAccountProviderException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * @return {@code true} iff the navigation drawer should show an indicator
   */

  protected abstract boolean navigationDrawerShouldShowIndicator();

  /**
   * @return The title string for this activity
   */

  protected abstract String navigationDrawerGetActivityTitle(Resources resources);

  protected final FrameLayout getContentFrame() {
    return NullCheck.notNull(this.content_frame);
  }

  private void hideKeyboard() {
    // Check if no view has focus:
    final View view = this.getCurrentFocus();
    if (view != null) {
      final InputMethodManager im = (InputMethodManager) this.getSystemService(
        Context.INPUT_METHOD_SERVICE);
      im.hideSoftInputFromWindow(
        view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
  }

  /**
   * Configure the action bar title. Subclasses are given an opportunity to set their own title
   * via the {@link #navigationDrawerGetActivityTitle(Resources)} method.
   */

  private void setActionBarTitle() {
    final ActionBar bar = NullCheck.notNull(this.getSupportActionBar());
    final String title = this.navigationDrawerGetActivityTitle(this.getResources());
    bar.setTitle(title);
    this.setTitle(title);
  }

  @Override
  public void onBackPressed() {
    LOG.debug("onBackPressed: {}", this);

    final ActionBar bar = this.getSupportActionBar();
    final Resources resources = this.getResources();
    if (this.drawer_layout.isDrawerOpen(GravityCompat.START)) {
      LOG.debug("drawer is open: closing drawer and finishing activity");
      this.finishing = true;
      this.drawer_layout.closeDrawer(GravityCompat.START);
      bar.setHomeActionContentDescription(resources.getString(R.string.navigation_accessibility_drawer_show));
      return;
    }

    if (isLastActivity()) {
      LOG.debug("drawer is closed: last activity; opening drawer");
      this.drawer_layout.openDrawer(GravityCompat.START);
      bar.setHomeActionContentDescription(resources.getString(R.string.navigation_accessibility_drawer_hide));
      return;
    }

    LOG.debug("drawer is closed: not last activity; finishing activity");
    this.finish();
  }

  @Override
  protected void onCreate(final @Nullable Bundle state) {
    this.setTheme(Simplified.getCurrentTheme().getThemeWithActionBar());
    super.onCreate(state);

    LOG.debug("onCreate: {}", this);
    this.setContentView(R.layout.main);

    this.open_drawer = true;
    final Intent i = NullCheck.notNull(this.getIntent());
    LOG.debug("non-null intent");
    final Bundle a = i.getExtras();
    if (a != null) {
      LOG.debug("non-null intent extras");
      this.open_drawer = a.getBoolean(NAVIGATION_DRAWER_OPEN_ID);
      LOG.debug("drawer requested: {}", open_drawer);
    }

    /*
     * The activity is being re-initialized. Set the drawer to whatever
     * state it was in when the activity was destroyed.
     */

    if (state != null) {
      LOG.debug("reinitializing");
      this.open_drawer = state.getBoolean(NAVIGATION_DRAWER_OPEN_ID, open_drawer);
    }

    /*
     * As per the Android design documents: If the user has manually opened
     * the navigation drawer, then the user is assumed to understand how the
     * drawer works. Therefore, if it appears that the drawer should be
     * opened, check to see if it should actually be closed.
     *
     * XXX: Make this part of the profile preferences
     */

    final SharedPreferences in_drawer_settings =
      NullCheck.notNull(this.getSharedPreferences("drawer-settings", 0));

    if (in_drawer_settings.getBoolean("has-opened-manually", false)) {
      LOG.debug("user has manually opened drawer in the past, not opening it now!");
      this.open_drawer = false;
    }
    this.drawer_settings = in_drawer_settings;

    /*
     * Configure the navigation drawer.
     */

    this.drawer_layout =
      NullCheck.notNull(this.findViewById(R.id.drawer_layout));
    this.drawer_list_view =
      NullCheck.notNull(this.findViewById(R.id.left_drawer));
    this.content_frame =
      NullCheck.notNull(this.findViewById(R.id.content_frame));
  }

  @Override
  protected void onStart() {
    super.onStart();

    /*
     * If no profile is selected, then we are presumably recovering from a crash
     * and have been shoved back into the middle of the application without having
     * gone via the splash screen. The ProfileTimeOutActivity will take care of moving
     * back to the profile selection screen, but we need to avoid doing anything with
     * profiles here to ensure safe passage.
     */

    if (!Simplified.getProfilesController().profileAnyIsCurrent()) {
      LOG.debug("no profile is enabled, aborting!");
      return;
    }

    /*
     * Configure the navigation drawer.
     */

    this.drawer_layout.addDrawerListener(this);
    this.drawer_layout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    this.drawer_list_view.setOnItemClickListener(this);
    this.drawer_layout.setDrawerTitle(
      Gravity.LEFT, this.getResources().getString(R.string.navigation_accessibility));

    this.drawer_items_initial =
      calculateDrawerItemsInitial(this);
    this.drawer_adapter =
      NavigationDrawerArrayAdapter.create(this, drawer_list_view);
    this.drawer_adapter.getDrawerItems().addAll(this.drawer_items_initial);

    this.drawer_list_view.setAdapter(this.drawer_adapter);

    /*
     * Show or hide the three dashes next to the home button.
     */

    final ActionBar bar = NullCheck.notNull(this.getSupportActionBar(), "Action bar");
    if (this.navigationDrawerShouldShowIndicator()) {
      LOG.debug("setting navigation drawer indicator");
      bar.setDisplayOptions(
        ActionBar.DISPLAY_SHOW_TITLE
          | ActionBar.DISPLAY_HOME_AS_UP
          | ActionBar.DISPLAY_SHOW_HOME);
      bar.setHomeAsUpIndicator(R.drawable.ic_drawer);
    }

    /*
     * If the drawer should be open, open it.
     */

    if (open_drawer) {
      drawer_layout.openDrawer(GravityCompat.START);
      bar.setHomeActionContentDescription(
        this.getResources().getString(R.string.navigation_accessibility_drawer_hide));
    }

    this.profile_event_subscription =
      Simplified.getProfilesController()
        .profileEvents()
        .subscribe(this::onProfileEvent);

    this.setActionBarTitle();
  }

  @Override
  protected void onStop() {
    super.onStop();

    final ObservableSubscriptionType<ProfileEvent> subscription = this.profile_event_subscription;
    if (subscription != null) {
      subscription.unsubscribe();
    }
  }

  private void onProfileEvent(final ProfileEvent event) {
    if (event instanceof ProfileAccountSelectEvent) {
      final ProfileAccountSelectEvent event_select = (ProfileAccountSelectEvent) event;
      event_select.matchSelect(
        this::onProfileAccountSelectSucceeded,
        this::onProfileAccountSelectFailed);
      return;
    }
  }

  private Unit onProfileAccountSelectFailed(
    final ProfileAccountSelectEvent.ProfileAccountSelectFailed event) {

    LOG.debug("onProfileAccountSelectFailed: {}", event);
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(R.string.profiles_account_selection_error_general);
    builder.create().show();
    return Unit.unit();
  }

  private Unit onProfileAccountSelectSucceeded(
    final ProfileAccountSelectEvent.ProfileAccountSelectSucceeded event) {

    LOG.debug("onProfileAccountSelectSucceeded: {}", event);
    return Unit.unit();
  }

  @Override
  public final void onDrawerClosed(final @Nullable View drawer_view) {
    LOG.debug("onDrawerClosed");

    /*
     * Clear the selected item when the drawer closes.
     */

    this.drawer_list_view.clearChoices();

    /*
     * If the drawer is closing because the user pressed the back button, then
     * finish the activity.
     */

    if (this.finishing) {
      this.finish();
    }
  }

  @Override
  public final void onDrawerOpened(final @Nullable View drawer_view) {
    LOG.debug("onDrawerOpened: {}", drawer_view);

    final SharedPreferences in_drawer_settings = NullCheck.notNull(this.drawer_settings);
    in_drawer_settings.edit().putBoolean("has-opened-manually", true).apply();
    this.hideKeyboard();
  }

  @Override
  public final void onDrawerSlide(
    final @Nullable View drawer_view,
    final float slide_offset) {
    // Nothing
  }

  @Override
  public final void onDrawerStateChanged(final int new_state) {
    LOG.debug("onDrawerStateChanged: {}", new_state);
  }

  @Override
  public void onItemClick(
    final @Nullable AdapterView<?> parent,
    final @Nullable View view,
    final int position,
    final long id) {

    final NavigationDrawerItemType item = this.drawer_adapter.getItem(position);
    LOG.debug("onItemClick: {}", item);
    item.onSelect(this.drawer_layout, this.drawer_adapter);
  }

  @Override
  public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn) {
    final MenuItem item = NullCheck.notNull(item_mn);
    final Resources rr = NullCheck.notNull(this.getResources());

    switch (item.getItemId()) {
      case android.R.id.home: {
        final DrawerLayout d = NullCheck.notNull(this.drawer_layout);
        final ActionBar bar = this.getSupportActionBar();
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

  @Override
  protected void onSaveInstanceState(final @Nullable Bundle state) {
    super.onSaveInstanceState(state);

    /*
     * Save the state of the navigation drawer. The intention here is that
     * the drawer will be open or closed based on the state it was
     * in when the user left the activity. In practice, the drawer tends to
     * close whenever the user moves to a new activity, so saving and restoring
     * may be redundant.
     */

    final Bundle state_nn = NullCheck.notNull(state);
    final DrawerLayout d = NullCheck.notNull(this.drawer_layout);
    state_nn.putBoolean(NAVIGATION_DRAWER_OPEN_ID, d.isDrawerOpen(GravityCompat.START));
  }

  /**
   * The type of items that can appear in the navigation drawer.
   */

  private interface NavigationDrawerItemType {

    /**
     * Called when it is time to configure the icon and text for the item. {@code checked} is
     * set to {@code true} if the item is the currently selected item.
     */

    void onConfigureIconAndText(
      TextView text_view,
      ImageView icon_view,
      boolean checked);

    /**
     * Called when an item is selected. This is where any action such as spawning a new activity
     * should be performed.
     */

    void onSelect(
      DrawerLayout drawer,
      NavigationDrawerArrayAdapter array_adapter);

    /**
     * Called when a view should be created. This function is expected to behave in the manner
     * of a ListView adapter. That is, it should either return the {@code reuse} view, or it
     * should inflate a new drawer item view.
     */

    View onCreateView(ViewGroup parent, View reuse);
  }

  /**
   * An abstract drawer item to ease the creation of new items.
   */

  private static abstract class NavigationDrawerItem implements NavigationDrawerItemType {
    protected final Activity activity;

    NavigationDrawerItem(final Activity activity) {
      this.activity = NullCheck.notNull(activity, "activity");
    }

    @Override
    public View onCreateView(
      final ViewGroup parent,
      final View reuse) {
      if (reuse != null && reuse.getId() == R.id.drawer_item_plain) {
        return reuse;
      } else {
        final LayoutInflater inflater = this.activity.getLayoutInflater();
        return inflater.inflate(R.layout.drawer_item_account, parent, false);
      }
    }
  }

  /**
   * The drawer item that shows the current account for the profile. Selecting it replaces
   * the menu with a list of all of the accounts on the current profile so that one of them may
   * be selected instead.
   */

  private static final class NavigationDrawerItemAccountCurrent extends NavigationDrawerItem {

    NavigationDrawerItemAccountCurrent(final Activity activity) {
      super(activity);
    }

    @Override
    public void onConfigureIconAndText(
      final TextView text_view,
      final ImageView icon_view,
      final boolean checked) {

      try {
        final AccountProvider account =
          Simplified.getProfilesController()
            .profileAccountCurrent()
            .provider();

        text_view.setText(account.displayName());
        ImageAccountIcons.loadAccountLogoIntoView(
          Simplified.getLocalImageLoader(),
          account,
          icon_view);
      } catch (final ProfileNoneCurrentException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void onSelect(
      final DrawerLayout drawer,
      final NavigationDrawerArrayAdapter array_adapter) {

      final List<NavigationDrawerItemType> items = array_adapter.getDrawerItems();
      items.clear();
      items.addAll(calculateDrawerItemsAccounts(this.activity));
      array_adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(
      final ViewGroup parent,
      final View reuse) {
      final View view;
      if (reuse != null && reuse.getId() == R.id.drawer_item_current_account) {
        view = reuse;
      } else {
        final LayoutInflater inflater = this.activity.getLayoutInflater();
        view = inflater.inflate(R.layout.drawer_item_current_account, parent, false);
      }

      return view;
    }
  }

  /**
   * A link to the accounts activity.
   */

  private static final class NavigationDrawerItemAccountManage extends NavigationDrawerItem {

    NavigationDrawerItemAccountManage(final Activity activity) {
      super(activity);
    }

    @Override
    public void onConfigureIconAndText(
      final TextView text_view,
      final ImageView icon_view,
      final boolean checked) {
      text_view.setText(R.string.settings_manage_accounts);
      icon_view.setImageResource(R.drawable.menu_icon_settings);
    }

    @Override
    public void onSelect(
      final DrawerLayout drawer,
      final NavigationDrawerArrayAdapter array_adapter) {

      UIThread.checkIsUIThread();

      drawer.closeDrawer(GravityCompat.START);

      UIThread.runOnUIThreadDelayed(() -> {
        startActivityWithoutHistory(this.activity, new Bundle(), SettingsActivity.class);
      }, 500L);
    }
  }

  /**
   * A link to the My Books section.
   */

  private static final class NavigationDrawerItemBooks extends NavigationDrawerItem {

    NavigationDrawerItemBooks(final Activity activity) {
      super(activity);
    }

    @Override
    public void onConfigureIconAndText(
      final TextView text_view,
      final ImageView icon_view,
      final boolean checked) {

      text_view.setText(R.string.books);
      if (checked) {
        icon_view.setImageResource(R.drawable.menu_icon_books_white);
      } else {
        icon_view.setImageResource(R.drawable.menu_icon_books);
      }
    }

    @Override
    public void onSelect(
      final DrawerLayout drawer,
      final NavigationDrawerArrayAdapter array_adapter) {
      UIThread.checkIsUIThread();

      drawer.closeDrawer(GravityCompat.START);

      UIThread.runOnUIThreadDelayed(() -> {
        final Bundle bundle = new Bundle();
        final OptionType<String> no_search = Option.none();
        final ImmutableStack<CatalogFeedArguments> empty_stack = ImmutableStack.empty();
        final CatalogFeedArguments.CatalogFeedArgumentsLocalBooks local =
          new CatalogFeedArguments.CatalogFeedArgumentsLocalBooks(
            this.activity.getResources().getString(R.string.books),
            empty_stack,
            SORT_BY_TITLE,
            no_search,
            FeedBooksSelection.BOOKS_FEED_LOANED);
        CatalogFeedActivity.Companion.setActivityArguments(bundle, local);
        startActivityWithoutHistory(this.activity, bundle, MainBooksActivity.class);
      }, 500L);
    }
  }

  /**
   * A link to the Reservations section.
   */

  private static final class NavigationDrawerItemHolds extends NavigationDrawerItem {

    NavigationDrawerItemHolds(final Activity activity) {
      super(activity);
    }

    @Override
    public void onConfigureIconAndText(
      final TextView text_view,
      final ImageView icon_view,
      final boolean checked) {

      text_view.setText(R.string.holds);
      if (checked) {
        icon_view.setImageResource(R.drawable.menu_icon_holds_white);
      } else {
        icon_view.setImageResource(R.drawable.menu_icon_holds);
      }
    }

    @Override
    public void onSelect(
      final DrawerLayout drawer,
      final NavigationDrawerArrayAdapter array_adapter) {
      UIThread.checkIsUIThread();

      drawer.closeDrawer(GravityCompat.START);

      UIThread.runOnUIThreadDelayed(() -> {

        final Bundle bundle = new Bundle();
        final OptionType<String> no_search = Option.none();
        final ImmutableStack<CatalogFeedArguments> empty_stack = ImmutableStack.empty();
        final CatalogFeedArguments.CatalogFeedArgumentsLocalBooks local =
          new CatalogFeedArguments.CatalogFeedArgumentsLocalBooks(
            this.activity.getResources().getString(R.string.holds),
            empty_stack,
            SORT_BY_TITLE,
            no_search,
            FeedBooksSelection.BOOKS_FEED_HOLDS);
        CatalogFeedActivity.Companion.setActivityArguments(bundle, local);
        startActivityWithoutHistory(this.activity, bundle, MainHoldsActivity.class);
      }, 500L);
    }


  }

  private static void startActivityWithoutHistory(
    final Activity source,
    final Bundle bundle,
    final Class<? extends Activity> target) {

    bundle.putBoolean(NAVIGATION_DRAWER_OPEN_ID, false);
    final Intent intent = new Intent();
    intent.setClass(source, target);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtras(bundle);
    source.startActivity(intent);
  }

  private static final class NavigationDrawerItemCatalog extends NavigationDrawerItem {

    NavigationDrawerItemCatalog(final Activity activity) {
      super(activity);
    }

    @Override
    public void onConfigureIconAndText(
      final TextView text_view,
      final ImageView icon_view,
      final boolean checked) {

      text_view.setText(R.string.catalog);
      if (checked) {
        icon_view.setImageResource(R.drawable.menu_icon_catalog_white);
      } else {
        icon_view.setImageResource(R.drawable.menu_icon_catalog);
      }
    }

    @Override
    public void onSelect(
      final DrawerLayout drawer,
      final NavigationDrawerArrayAdapter array_adapter) {
      UIThread.checkIsUIThread();

      drawer.closeDrawer(GravityCompat.START);

      UIThread.runOnUIThreadDelayed(() -> {
        final Bundle bundle = new Bundle();
        MainCatalogActivity.setActivityArguments(bundle, false);
        startActivityWithoutHistory(this.activity, bundle, MainCatalogActivity.class);
      }, 500L);
    }
  }

  /**
   * A link to the Settings section.
   */

  private static final class NavigationDrawerItemSettings extends NavigationDrawerItem {

    NavigationDrawerItemSettings(final Activity activity) {
      super(activity);
    }

    @Override
    public void onConfigureIconAndText(
      final TextView text_view,
      final ImageView icon_view,
      final boolean checked) {

      text_view.setText(R.string.settings);
      if (checked) {
        icon_view.setImageResource(R.drawable.menu_icon_settings_white);
      } else {
        icon_view.setImageResource(R.drawable.menu_icon_settings);
      }
    }

    @Override
    public void onSelect(
      final DrawerLayout drawer,
      final NavigationDrawerArrayAdapter array_adapter) {
      UIThread.checkIsUIThread();

      drawer.closeDrawer(GravityCompat.START);

      UIThread.runOnUIThreadDelayed(() -> {
        startActivityWithoutHistory(this.activity, new Bundle(), SettingsActivity.class);
      }, 500L);
    }
  }

  /**
   * An item that moves back to the profile selection screen. This is only present if profiles
   * are actually enabled.
   */

  private static final class NavigationDrawerItemSwitchProfile extends NavigationDrawerItem {

    NavigationDrawerItemSwitchProfile(final Activity activity) {
      super(activity);
    }

    @Override
    public void onConfigureIconAndText(
      final TextView text_view,
      final ImageView icon_view,
      final boolean checked) {

      text_view.setText(R.string.profiles_switch);
      icon_view.setImageResource(R.drawable.menu_icon_profile_logout);
    }

    @Override
    public void onSelect(
      final DrawerLayout drawer,
      final NavigationDrawerArrayAdapter array_adapter) {
      UIThread.checkIsUIThread();

      final ProfileSwitchDialog dialog =
        ProfileSwitchDialog.newDialog(() -> {
          drawer.closeDrawer(GravityCompat.START);

          UIThread.runOnUIThreadDelayed(() -> {
            startActivityWithoutHistory(this.activity, new Bundle(), ProfileSelectionActivity.class);
          }, 500L);
        });

      dialog.show(this.activity.getFragmentManager(), "profile-switch-dialog");
    }
  }

  /**
   * An item that selects a specific account.
   *
   * @see NavigationDrawerItemAccountCurrent
   */

  private static final class NavigationDrawerItemAccountSelectSpecific extends NavigationDrawerItem {

    private final AccountProvider account;

    NavigationDrawerItemAccountSelectSpecific(
      final Activity activity,
      final AccountProvider account) {
      super(activity);
      this.account = NullCheck.notNull(account, "account");
    }

    @Override
    public void onConfigureIconAndText(
      final TextView text_view,
      final ImageView icon_view,
      final boolean checked) {

      text_view.setText(this.account.displayName());
      ImageAccountIcons.loadAccountLogoIntoView(
        Simplified.getLocalImageLoader(),
        this.account,
        icon_view);
    }

    @Override
    public void onSelect(
      final DrawerLayout drawer,
      final NavigationDrawerArrayAdapter array_adapter) {
      UIThread.checkIsUIThread();

      drawer.closeDrawer(GravityCompat.START);

      UIThread.runOnUIThreadDelayed(() -> {
        Simplified.getProfilesController().profileAccountSelectByProvider(this.account.id());

        final List<NavigationDrawerItemType> items = array_adapter.getDrawerItems();
        items.clear();
        items.addAll(calculateDrawerItemsInitial(this.activity));
        array_adapter.notifyDataSetChanged();
      }, 500L);
    }
  }

  /**
   * The standard array adapter for the navigation drawer.
   */

  private static final class NavigationDrawerArrayAdapter
    extends ArrayAdapter<NavigationDrawerItemType> {

    private final ArrayList<NavigationDrawerItemType> drawer_items;
    private final ListView drawer_list_view;

    private NavigationDrawerArrayAdapter(
      final NavigationDrawerActivity activity,
      final ArrayList<NavigationDrawerItemType> drawer_items,
      final ListView drawer_list_view) {

      super(activity, R.layout.drawer_item, drawer_items);

      this.drawer_items =
        NullCheck.notNull(drawer_items, "drawer_items");
      this.drawer_list_view =
        NullCheck.notNull(drawer_list_view, "drawer_list_view");
    }

    static NavigationDrawerArrayAdapter create(
      final NavigationDrawerActivity activity,
      final ListView drawer_list_view) {
      final ArrayList<NavigationDrawerItemType> drawer_items = new ArrayList<>(32);
      return new NavigationDrawerArrayAdapter(activity, drawer_items, drawer_list_view);
    }

    public List<NavigationDrawerItemType> getDrawerItems() {
      return this.drawer_items;
    }

    @Override
    public View getView(
      final int position,
      final @Nullable View reuse,
      final @Nullable ViewGroup parent) {

      final NavigationDrawerItemType item = this.drawer_items.get(position);
      final View view = item.onCreateView(parent, reuse);

      final TextView text_view =
        NullCheck.notNull(view.findViewById(android.R.id.text1));
      final ImageView icon_view =
        NullCheck.notNull(view.findViewById(R.id.cellIcon));

      item.onConfigureIconAndText(
        text_view,
        icon_view,
        this.drawer_list_view.getCheckedItemPosition() == position);

      return view;
    }
  }
}
