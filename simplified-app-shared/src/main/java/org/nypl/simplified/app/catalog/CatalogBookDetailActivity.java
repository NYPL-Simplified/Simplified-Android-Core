package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.jetbrains.annotations.NotNull;
import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException;
import org.nypl.simplified.app.NavigationDrawerActivity;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.login.LoginDialogListenerType;
import org.nypl.simplified.app.services.SimplifiedServicesType;
import org.nypl.simplified.books.book_registry.BookRegistryReadableType;
import org.nypl.simplified.books.book_registry.BookStatusEvent;
import org.nypl.simplified.observable.ObservableSubscriptionType;
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException;
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType;
import org.nypl.simplified.stack.ImmutableStack;

import static org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS;

/**
 * An activity showing a full-screen book detail page.
 */

public final class CatalogBookDetailActivity extends CatalogActivity implements LoginDialogListenerType {
  private static final String CATALOG_BOOK_DETAIL_FEED_ENTRY_ID;

  static {
    CATALOG_BOOK_DETAIL_FEED_ENTRY_ID =
      "org.nypl.simplified.app.CatalogBookDetailActivity.feed_entry";
  }

  private CatalogBookDetailView view;
  private ObservableSubscriptionType<BookStatusEvent> bookSubscription;

  /**
   * Construct an activity.
   */

  public CatalogBookDetailActivity() {

  }

  /**
   * Set the arguments of the activity to be created.
   *
   * @param b           The argument bundle
   * @param drawer_open {@code true} if the navigation drawer should be opened.
   * @param up_stack    The up-stack
   * @param e           The feed entry
   */

  public static void setActivityArguments(
    final Bundle b,
    final boolean drawer_open,
    final ImmutableStack<CatalogFeedArguments> up_stack,
    final FeedEntryOPDS e) {
    NullCheck.notNull(b);
    NavigationDrawerActivity.setActivityArguments(b, drawer_open);
    CatalogActivity.Companion.setActivityArguments(b, up_stack);
    b.putSerializable(CATALOG_BOOK_DETAIL_FEED_ENTRY_ID, NullCheck.notNull(e));
  }

  /**
   * Start a new activity with the given arguments.
   *
   * @param from     The parent activity
   * @param up_stack The up stack
   * @param e        The feed entry
   */

  public static void startNewActivity(
    final Activity from,
    final ImmutableStack<CatalogFeedArguments> up_stack,
    final FeedEntryOPDS e) {
    final Bundle b = new Bundle();
    setActivityArguments(b, false, up_stack, e);
    final Intent i = new Intent(from, CatalogBookDetailActivity.class);
    i.putExtras(b);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    from.startActivity(i);
  }

  private FeedEntryOPDS getFeedEntry() {
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = NullCheck.notNull(i.getExtras());
    return NullCheck.notNull(
      (FeedEntryOPDS) a.getSerializable(CATALOG_BOOK_DETAIL_FEED_ENTRY_ID));
  }

  @Override
  protected String navigationDrawerGetActivityTitle(final Resources resources) {
    return resources.getString(R.string.catalog_book_detail);
  }

  @Override
  protected boolean navigationDrawerShouldShowIndicator() {
    return this.upStack.isEmpty();
  }

  @Override
  protected void onCreate(final @Nullable Bundle state) {
    super.onCreate(state);

    final SimplifiedServicesType services =
      Simplified.getServices();
    final BookRegistryReadableType bookRegistry =
      services.getBookRegistry();
    final ProfilesControllerType profiles =
      services.getProfilesController();

    final FeedEntryOPDS entry = this.getFeedEntry();
    final AccountType account;
    try {
      account = profiles.profileAccountForBook(entry.getBookID());
    } catch (final ProfileNoneCurrentException | AccountsDatabaseNonexistentException e) {
      throw new IllegalStateException(e);
    }

    final LayoutInflater inflater =
      NullCheck.notNull(this.getLayoutInflater());

    final CatalogBookDetailView detailView =
      new CatalogBookDetailView(
        this,
        inflater,
        account,
        services.getBookCovers(),
        bookRegistry,
        services.getAnalytics(),
        profiles,
        services.getBooksController(),
        services.getScreenSize(),
        services.getNetworkConnectivity(),
        entry);

    this.view = detailView;

    final FrameLayout content_area = this.getContentFrame();
    content_area.removeAllViews();
    content_area.addView(detailView.getScrollView());
    content_area.requestLayout();
  }

  @Override
  protected void onStart() {
    super.onStart();
    this.navigationDrawerShowUpIndicatorUnconditionally();

    /*
     * Subscribe the detail view to book events.
     */

    this.bookSubscription =
      Simplified.getServices()
        .getBookRegistry()
        .bookEvents()
        .subscribe(this.view::onBookEvent);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    this.bookSubscription.unsubscribe();
  }

  @NotNull
  @Override
  public ProfilesControllerType onLoginDialogWantsProfilesController() {
    return Simplified.getServices()
      .getProfilesController();
  }
}
