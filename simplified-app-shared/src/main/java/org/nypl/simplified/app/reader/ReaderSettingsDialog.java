package org.nypl.simplified.app.reader;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.ScreenSizeInformationType;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.books.controller.ProfilesControllerType;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.books.reader.ReaderColorScheme;
import org.nypl.simplified.books.reader.ReaderFontSelection;
import org.nypl.simplified.books.reader.ReaderPreferences;

/**
 * The reader settings dialog, allowing the selection of colors and fonts.
 */

public final class ReaderSettingsDialog extends DialogFragment {

  private ProfilesControllerType profiles;
  private ReaderPreferences.Builder reader_preferences_builder;

  /**
   * Construct a dialog.
   */

  public ReaderSettingsDialog() {

  }

  @Override
  public void onCreate(final @Nullable Bundle state) {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedBookDialog);
  }

  @Override
  public View onCreateView(
      final @Nullable LayoutInflater inflater_mn,
      final @Nullable ViewGroup container,
      final @Nullable Bundle state) {

    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);
    final LinearLayout layout = NullCheck.notNull(
        (LinearLayout) inflater.inflate(
            R.layout.reader_settings, container, false));

    final TextView in_view_font_serif =
        NullCheck.notNull(layout.findViewById(R.id.reader_settings_font_serif));
    final TextView in_view_font_sans =
        NullCheck.notNull(layout.findViewById(R.id.reader_settings_font_sans));
    final TextView in_view_font_open_dyslexic =
        NullCheck.notNull(layout.findViewById(R.id.reader_settings_font_open_dyslexic));

    final TextView in_view_black_on_white =
        NullCheck.notNull(layout.findViewById(R.id.reader_settings_black_on_white));
    final TextView in_view_white_on_black =
        NullCheck.notNull(layout.findViewById(R.id.reader_settings_white_on_black));
    final TextView in_view_black_on_beige =
        NullCheck.notNull(layout.findViewById(R.id.reader_settings_black_on_beige));

    final TextView in_view_text_smaller =
        NullCheck.notNull(layout.findViewById(R.id.reader_settings_text_smaller));
    final TextView in_view_text_larger =
        NullCheck.notNull(layout.findViewById(R.id.reader_settings_text_larger));

    final SeekBar in_view_brightness =
        NullCheck.notNull(layout.findViewById(R.id.reader_settings_brightness));
    final Button in_view_close_button =
        NullCheck.notNull(layout.findViewById(R.id.reader_settings_close));

    try {
      final ReaderPreferences reader_preferences =
          this.profiles.profileCurrent()
              .preferences()
              .readerPreferences();

      this.reader_preferences_builder = reader_preferences.toBuilder();
    } catch (final ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    }

    /*
     * Configure the settings buttons.
     */

    in_view_font_serif.setOnClickListener(view -> {
      this.reader_preferences_builder.setFontFamily(ReaderFontSelection.READER_FONT_SERIF);
      this.updatePreferences(this.reader_preferences_builder.build());
    });
    in_view_font_sans.setOnClickListener(view -> {
      this.reader_preferences_builder.setFontFamily(ReaderFontSelection.READER_FONT_SANS_SERIF);
      this.updatePreferences(this.reader_preferences_builder.build());
    });
    in_view_font_open_dyslexic.setOnClickListener(view -> {
      this.reader_preferences_builder.setFontFamily(ReaderFontSelection.READER_FONT_OPEN_DYSLEXIC);
      this.updatePreferences(this.reader_preferences_builder.build());
    });

    final Typeface od = Typeface.createFromAsset(
        this.getActivity().getAssets(), "OpenDyslexic3-Regular.ttf");
    in_view_font_open_dyslexic.setTypeface(od);

    in_view_black_on_white.setBackgroundColor(
       ReaderColorSchemes.background(ReaderColorScheme.SCHEME_BLACK_ON_WHITE));
    in_view_black_on_white.setTextColor(
        ReaderColorSchemes.foreground(ReaderColorScheme.SCHEME_BLACK_ON_WHITE));

    in_view_black_on_white.setOnClickListener(view -> {
      this.reader_preferences_builder.setColorScheme(ReaderColorScheme.SCHEME_BLACK_ON_WHITE);
      this.updatePreferences(this.reader_preferences_builder.build());
    });

    in_view_white_on_black.setBackgroundColor(
        ReaderColorSchemes.background(ReaderColorScheme.SCHEME_WHITE_ON_BLACK));
    in_view_white_on_black.setTextColor(
        ReaderColorSchemes.foreground(ReaderColorScheme.SCHEME_WHITE_ON_BLACK));

    in_view_white_on_black.setOnClickListener(view -> {
      this.reader_preferences_builder.setColorScheme(ReaderColorScheme.SCHEME_WHITE_ON_BLACK);
      this.updatePreferences(this.reader_preferences_builder.build());
    });

    in_view_black_on_beige.setBackgroundColor(
        ReaderColorSchemes.background(ReaderColorScheme.SCHEME_BLACK_ON_BEIGE));
    in_view_black_on_beige.setTextColor(
        ReaderColorSchemes.foreground(ReaderColorScheme.SCHEME_BLACK_ON_BEIGE));

    in_view_black_on_beige.setOnClickListener(view -> {
      this.reader_preferences_builder.setColorScheme(ReaderColorScheme.SCHEME_BLACK_ON_BEIGE);
      this.updatePreferences(this.reader_preferences_builder.build());
    });

    in_view_text_larger.setOnClickListener(view -> {
      if (this.reader_preferences_builder.fontScale() < 250) {
        this.reader_preferences_builder.setFontScale(this.reader_preferences_builder.fontScale() + 25.0f);
        this.updatePreferences(this.reader_preferences_builder.build());
      }
    });

    in_view_text_smaller.setOnClickListener(view -> {
      if (this.reader_preferences_builder.fontScale() > 75) {
        this.reader_preferences_builder.setFontScale(this.reader_preferences_builder.fontScale() - 25.0f);
        this.updatePreferences(this.reader_preferences_builder.build());
      }
    });

    in_view_close_button.setOnClickListener(view -> this.dismiss());

    /*
     * Configure brightness controller.
     */

    in_view_brightness.setProgress((int) (this.reader_preferences_builder.brightness() * 100));
    in_view_brightness.setOnSeekBarChangeListener(
        new OnSeekBarChangeListener() {

          private double bright = 0.5;

          @Override
          public void onProgressChanged(
              final @Nullable SeekBar bar,
              final int progress,
              final boolean from_user) {

            this.bright = progress / 100.0;
          }

          @Override
          public void onStartTrackingTouch(final @Nullable SeekBar bar) {

          }

          @Override
          public void onStopTrackingTouch(final @Nullable SeekBar bar) {
            updateBrightness((float) this.bright);
          }
        });

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }

    return layout;
  }

  private void updateBrightness(final float back_light_value) {
    final WindowManager.LayoutParams layout_params = getActivity().getWindow().getAttributes();
    layout_params.screenBrightness = back_light_value;
    getActivity().getWindow().setAttributes(layout_params);

    this.reader_preferences_builder.setBrightness(back_light_value);
    this.updatePreferences(this.reader_preferences_builder.build());
  }

  private void updatePreferences(final ReaderPreferences prefs) {
    try {
      this.profiles.profilePreferencesUpdate(
          this.profiles.profileCurrent()
              .preferences()
              .toBuilder()
              .setReaderPreferences(prefs)
              .build());
    } catch (final ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    final ScreenSizeInformationType screen = Simplified.getScreenSizeInformation();
    final Window window = this.getDialog().getWindow();

    window.setLayout(
        (int) screen.screenDPToPixels(300),
        ViewGroup.LayoutParams.WRAP_CONTENT);
    window.setGravity(Gravity.CENTER);
  }

  public static ReaderSettingsDialog create(final ProfilesControllerType profiles) {
    final ReaderSettingsDialog dialog = new ReaderSettingsDialog();
    dialog.setRequiredArguments(NullCheck.notNull(profiles, "Profiles"));
    return dialog;
  }

  private void setRequiredArguments(final ProfilesControllerType profiles) {
    this.profiles = NullCheck.notNull(profiles, "Profiles");
  }
}
