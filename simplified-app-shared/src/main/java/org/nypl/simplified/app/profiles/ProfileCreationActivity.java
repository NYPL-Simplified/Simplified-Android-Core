package org.nypl.simplified.app.profiles;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.joda.time.LocalDate;
import org.nypl.simplified.accounts.source.spi.AccountProviderRegistryType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.services.SimplifiedServicesType;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.datepicker.DatePicker;
import org.nypl.simplified.profiles.api.ProfileCreationEvent;
import org.nypl.simplified.profiles.api.ProfileEvent;
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationFailed;
import static org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationSucceeded;

/**
 * An activity that allows for the creation of profiles.
 */

public final class ProfileCreationActivity extends SimplifiedActivity implements TextWatcher {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileCreationActivity.class);

  private Button button;
  private DatePicker date;
  private EditText name;
  private RadioGroup genderRadioGroup;
  private RadioButton nonBinaryRadioButton;
  private EditText nonBinaryEditText;

  public ProfileCreationActivity() {

  }

  @Override
  protected void onCreate(final @Nullable Bundle state) {
    // This activity is too tall for many phones in landscape mode.
    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    this.setTheme(
      Simplified.getServices()
        .getCurrentTheme()
        .getThemeWithNoActionBar());
    super.onCreate(state);
    this.setContentView(R.layout.profiles_creation);

    this.button = NullCheck.notNull(this.findViewById(R.id.profileCreationCreate));
    this.button.setEnabled(false);
    this.button.setOnClickListener(view -> {
      view.setEnabled(false);
      createProfile();
    });

    this.date = NullCheck.notNull(this.findViewById(R.id.profileCreationDateSelection));
    this.name = NullCheck.notNull(this.findViewById(R.id.profileCreationEditName));
    this.genderRadioGroup = NullCheck.notNull(this.findViewById(R.id.profileGenderRadioGroup));
    this.nonBinaryRadioButton = NullCheck.notNull(
      this.findViewById(R.id.profileGenderNonBinaryRadioButton));
    this.nonBinaryEditText =
      NullCheck.notNull(this.findViewById(R.id.profileGenderNonBinaryEditText));

    this.name.addTextChangedListener(this);
    this.nonBinaryEditText.addTextChangedListener(this);
    this.nonBinaryEditText.setOnFocusChangeListener((View view, boolean hasFocus) -> {
      if (hasFocus) {
        this.nonBinaryRadioButton.setChecked(true);
      }
    });
    this.genderRadioGroup.setOnCheckedChangeListener((group, id) -> {
      if (id == R.id.profileGenderNonBinaryRadioButton) {
        this.nonBinaryEditText.requestFocus();
      } else {
        this.nonBinaryEditText.clearFocus();
      }
      this.updateButtonEnabled();
    });
  }

  private Unit onProfileCreationFailed(final ProfileCreationFailed e) {
    LOG.debug("onProfileCreationFailed: {}", e);

    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      LOG,
      this.getResources().getString(messageForErrorCode(e.errorCode())),
      null,
      () -> this.button.setEnabled(true));

    return Unit.unit();
  }

  private int messageForErrorCode(final ProfileCreationFailed.ErrorCode code) {
    switch (code) {
      case ERROR_DISPLAY_NAME_ALREADY_USED:
        return R.string.profiles_creation_error_name_already_used;
      case ERROR_GENERAL:
        return R.string.profiles_creation_error_general;
    }
    throw new UnreachableCodeException();
  }

  private Unit onProfileCreationSucceeded(final ProfileCreationSucceeded e) {
    LOG.debug("onProfileCreationSucceeded: {}", e);
    UIThread.runOnUIThread(this::openSelectionActivity);
    return Unit.unit();
  }

  private Unit onProfileEvent(final ProfileEvent event) {
    LOG.debug("onProfileEvent: {}", event);
    if (event instanceof ProfileCreationEvent) {
      final ProfileCreationEvent event_create = (ProfileCreationEvent) event;
      return event_create.matchCreation(
        this::onProfileCreationSucceeded,
        this::onProfileCreationFailed);
    }
    return Unit.unit();
  }

  private void openSelectionActivity() {
    final Intent i = new Intent(this, ProfileSelectionActivity.class);
    this.startActivity(i);
    this.finish();
  }

  private void createProfile() {
    final String name_text = name.getText().toString().trim();
    final String gender_text;
    if (this.genderRadioGroup.getCheckedRadioButtonId() == R.id.profileGenderFemaleRadioButton) {
      gender_text = "female";
    } else if (this.genderRadioGroup.getCheckedRadioButtonId() == R.id.profileGenderMaleRadioButton) {
      gender_text = "male";
    } else if (this.genderRadioGroup.getCheckedRadioButtonId() == R.id.profileGenderNonBinaryRadioButton) {
      gender_text = this.nonBinaryEditText.getText().toString().toLowerCase().trim();
    } else {
      throw new RuntimeException("createProfile");
    }
    final LocalDate date_value = this.date.getDate();
    LOG.debug("name: {}", name_text);
    LOG.debug("gender: {}", gender_text);
    LOG.debug("date: {}", date_value);

    final SimplifiedServicesType services =
      Simplified.getServices();
    final AccountProviderRegistryType providers =
      services.getAccountProviderRegistry();
    final ProfilesControllerType profiles =
      services.getProfilesController();
    final ListeningExecutorService exec =
      services.getBackgroundExecutor();

    final ListenableFuture<ProfileCreationEvent> task =
      profiles.profileCreate(
        providers.getDefaultProvider(),
        name_text,
        gender_text,
        date_value);

    FluentFuture.from(task)
      .catching(Exception.class, e -> ProfileCreationFailed.of(name_text, ProfileCreationFailed.ErrorCode.ERROR_GENERAL, Option.some(e)), exec)
      .transform(this::onProfileEvent, exec);
  }

  private void updateButtonEnabled() {
    final boolean isNameEmpty = this.name.getText().toString().trim().isEmpty();
    final boolean isNonBinaryEmpty = this.nonBinaryEditText.getText().toString().trim().isEmpty();
    final boolean isAnyRadioButtonChecked = this.genderRadioGroup.getCheckedRadioButtonId() != -1;
    final boolean isNonBinaryRatioButtonChecked = this.nonBinaryRadioButton.isChecked();

    if (isNonBinaryRatioButtonChecked) {
      this.button.setEnabled(!isNameEmpty && !isNonBinaryEmpty);
    } else if (isAnyRadioButtonChecked) {
      this.button.setEnabled(!isNameEmpty);
    } else {
      this.button.setEnabled(false);
    }
  }

  @Override
  public void beforeTextChanged(
    final CharSequence text,
    final int i,
    final int i1,
    final int i2) {
  }

  @Override
  public void onTextChanged(
    final CharSequence text,
    final int i,
    final int i1,
    final int i2) {
    this.updateButtonEnabled();
  }

  @Override
  public void afterTextChanged(final Editable editable) {

  }
}
