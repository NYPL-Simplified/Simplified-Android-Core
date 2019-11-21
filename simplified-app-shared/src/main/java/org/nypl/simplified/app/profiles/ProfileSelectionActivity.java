package org.nypl.simplified.app.profiles;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.joda.time.LocalDateTime;
import org.librarysimplified.services.api.ServiceDirectoryType;
import org.nypl.simplified.analytics.api.AnalyticsEvent;
import org.nypl.simplified.analytics.api.AnalyticsType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;
import org.nypl.simplified.app.utilities.UIBackgroundExecutorType;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException;
import org.nypl.simplified.profiles.api.ProfilePreferences;
import org.nypl.simplified.profiles.api.ProfileReadableType;
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType;
import org.nypl.simplified.ui.theme.ThemeServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.disposables.Disposable;

/**
 * An activity that allows users to pick from a list of profiles, or to create a new profile.
 */

public final class ProfileSelectionActivity extends SimplifiedActivity {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileSelectionActivity.class);

  private Button button;
  private ListView list;
  private ProfileArrayAdapter list_adapter;
  private ArrayList<ProfileReadableType> list_items;
  private Disposable profile_event_subscription;

  public ProfileSelectionActivity() {

  }

  @Override
  protected void onCreate(final @Nullable Bundle state) {
    final ServiceDirectoryType services = Simplified.getServices();
    final int theme =
      services.requireService(ThemeServiceType.class)
        .findCurrentTheme()
        .getThemeWithActionBar();

    this.setTheme(theme);
    super.onCreate(state);

    this.setContentView(R.layout.profiles_selection);

    this.list_items = new ArrayList<>();
    this.reloadProfiles();

    this.list_adapter = new ProfileArrayAdapter(this, this.list_items);
    this.list = NullCheck.notNull(this.findViewById(R.id.profileSelectionList));
    this.list.setAdapter(this.list_adapter);
    this.list.setOnItemClickListener(
      (adapter_view, view, position, id) ->
        onSelectedProfile(NullCheck.notNull(list_items.get(position))));

    this.button = NullCheck.notNull(this.findViewById(R.id.profileSelectionCreate));
    this.button.setOnClickListener(view -> openCreationDialog());

    final ProfilesControllerType profiles =
      services.requireService(ProfilesControllerType.class);

    profiles.profileIdleTimer().stop();

    this.profile_event_subscription = profiles.profileEvents().subscribe(event -> reloadProfiles());

    /*
     * If the profile selection screen has been reached and a profile is active, then
     * assume that we've gotten here because a profile has been logged out.
     */

    if (profiles.profileAnyIsCurrent()) {
      try {
        final ProfileReadableType profile = profiles.profileCurrent();
        services
          .requireService(AnalyticsType.class)
          .publishEvent(
            new AnalyticsEvent.ProfileLoggedOut(
              LocalDateTime.now(),
              null,
              profile.getId().getUuid(),
              profile.getDisplayName()
            ));
      } catch (ProfileNoneCurrentException e) {
        LOG.error("profile is not current: ", e);
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    this.profile_event_subscription.dispose();
  }

  private <A> A getWithDefault(OptionType<A> optional, A orElse) {
    return optional instanceof Some ? ((Some<A>) optional).get() : orElse;
  }

  private void onSelectedProfile(
    final ProfileReadableType profile) {

    final ServiceDirectoryType services = Simplified.getServices();
    LOG.debug("selected profile: {} ({})", profile.getId(), profile.getDisplayName());
    final ProfilesControllerType profiles = services.requireService(ProfilesControllerType.class);

    final ProfilePreferences preferences =
      profile.preferences();
    final String gender =
      getWithDefault(preferences.gender(), null);
    final String birthday =
      getWithDefault(preferences.dateOfBirth()
        .map(date -> date.component1().toString()), null);

    services.requireService(AnalyticsType.class)
      .publishEvent(new AnalyticsEvent.ProfileLoggedIn(
        LocalDateTime.now(),
        null,
        profile.getId().getUuid(),
        profile.getDisplayName(),
        gender,
        birthday
      ));

    FluentFuture.from(
      profiles.profileSelect(profile.getId()))
      .addCallback(new FutureCallback<kotlin.Unit>() {
        @Override
        public void onSuccess(final kotlin.Unit result) {
          onProfileSelectionSucceeded(result);
        }

        @Override
        public void onFailure(final Throwable e) {
          LOG.error("profile selection failed: ", e);
          onProfileSelectionFailed(e);
        }
      }, services.requireService(UIBackgroundExecutorType.class));
  }

  private void onProfileSelectionFailed(final Throwable e) {
    LOG.error("onProfileSelectionFailed: {}", e);

    /*
     * XXX: What exactly can anyone do about this? It's made worse by the fact that if this
     * happens here, the user can't even get into the program to report a bug directly...
     */

    ErrorDialogUtilities.showError(
      this,
      LOG,
      this.getResources().getString(R.string.profiles_selection_error_general),
      null);
  }

  private void onProfileSelectionSucceeded(final kotlin.Unit ignored) {
    LOG.debug("onProfileSelectionSucceeded");
    LOG.debug("starting profile idle timer");
    Simplified.getServices()
      .requireService(ProfilesControllerType.class)
      .profileIdleTimer()
      .start();
    UIThread.runOnUIThread(this::openCatalog);
  }

  private void reloadProfiles() {
    LOG.debug("reloading profiles");

    UIThread.runOnUIThread(() -> {
      final ProfilesControllerType profiles =
        Simplified.getServices()
          .requireService(ProfilesControllerType.class);

      list_items.clear();
      list_items.addAll(profiles.profiles().values());
      Collections.sort(list_items);
      list_adapter.notifyDataSetChanged();
    });
  }

  private static final class ProfileArrayAdapter extends ArrayAdapter<ProfileReadableType> {

    private final List<ProfileReadableType> list_items;
    private final Context context;

    ProfileArrayAdapter(
      final Context in_context,
      final ArrayList<ProfileReadableType> objects) {
      super(in_context, R.layout.profiles_list_item, objects);
      this.context = NullCheck.notNull(in_context, "Context");
      this.list_items = NullCheck.notNull(objects, "Objects");
    }

    private static final class ViewHolder {
      private TextView text;
      private ImageView image;

      ViewHolder() {

      }
    }

    @Override
    public View getView(
      final int position,
      final View convert_view,
      final ViewGroup parent_group) {

      View row_view = convert_view;
      if (row_view == null) {
        final LayoutInflater inflater =
          (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        row_view = inflater.inflate(R.layout.profiles_list_item, null);

        final ViewHolder view_holder = new ViewHolder();
        view_holder.text = row_view.findViewById(R.id.profileItemDisplayName);
        view_holder.image = row_view.findViewById(R.id.profileItemIcon);
        row_view.setTag(view_holder);
      }

      final ViewHolder holder = (ViewHolder) row_view.getTag();
      final ProfileReadableType profile = this.list_items.get(position);
      holder.text.setText(profile.getDisplayName());
      LOG.trace("getView: [{}] profile: {}", position, profile.getDisplayName());
      return row_view;
    }
  }

  private void openCatalog() {
    final Intent i = new Intent(this, MainCatalogActivity.class);
    this.startActivity(i);
    this.finish();
  }

  private void openCreationDialog() {
    final Intent i = new Intent(this, ProfileCreationActivity.class);
    this.startActivity(i);
  }
}
