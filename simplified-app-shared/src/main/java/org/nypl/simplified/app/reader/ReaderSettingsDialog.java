package org.nypl.simplified.app.reader;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;

/**
 * The reader settings dialog, allowing the selection of colors and fonts.
 */

public final class ReaderSettingsDialog extends DialogFragment
{
  /**
   * Construct a dialog.
   */

  public ReaderSettingsDialog()
  {

  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedBookDialog);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater_mn,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);
    final LinearLayout layout = NullCheck.notNull(
      (LinearLayout) inflater.inflate(
        R.layout.reader_settings, container, false));

    final TextView in_view_font_serif = NullCheck.notNull(
      (TextView) layout.findViewById(
        R.id.reader_settings_font_serif));
    final TextView in_view_font_sans = NullCheck.notNull(
      (TextView) layout.findViewById(
        R.id.reader_settings_font_sans));
    final TextView in_view_font_open_dyslexic = NullCheck.notNull(
      (TextView) layout.findViewById(
        R.id.reader_settings_font_open_dyslexic));

    final TextView in_view_black_on_white = NullCheck.notNull(
      (TextView) layout.findViewById(
        R.id.reader_settings_black_on_white));
    final TextView in_view_white_on_black = NullCheck.notNull(
      (TextView) layout.findViewById(
        R.id.reader_settings_white_on_black));
    final TextView in_view_black_on_beige = NullCheck.notNull(
      (TextView) layout.findViewById(
        R.id.reader_settings_black_on_beige));

    final TextView in_view_text_smaller = NullCheck.notNull(
      (TextView) layout.findViewById(
        R.id.reader_settings_text_smaller));
    final TextView in_view_text_larger = NullCheck.notNull(
      (TextView) layout.findViewById(
        R.id.reader_settings_text_larger));

    final SeekBar in_view_brightness = NullCheck.notNull(
      (SeekBar) layout.findViewById(
        R.id.reader_settings_brightness));

    /**
     * Configure the settings buttons.
     */

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();
    final ReaderSettingsType settings = rs.getSettings();

    in_view_font_serif.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(final View v)
        {
          settings.setFontFamily(ReaderFontSelection.READER_FONT_SERIF);
        }
      });
    in_view_font_sans.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(final View v)
        {
          settings.setFontFamily(ReaderFontSelection.READER_FONT_SANS_SERIF);
        }
      });
    in_view_font_open_dyslexic.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(final View v)
        {
          settings.setFontFamily(ReaderFontSelection.READER_FONT_OPEN_DYSLEXIC);
        }
      });

    final Typeface od = Typeface.createFromAsset(
      this.getActivity().getAssets(), "OpenDyslexic3-Regular.ttf");
    in_view_font_open_dyslexic.setTypeface(od);

    in_view_black_on_white.setBackgroundColor(
      ReaderColorScheme.SCHEME_BLACK_ON_WHITE.getBackgroundColor());
    in_view_black_on_white.setTextColor(
      ReaderColorScheme.SCHEME_BLACK_ON_WHITE.getForegroundColor());
    in_view_black_on_white.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          settings.setColorScheme(ReaderColorScheme.SCHEME_BLACK_ON_WHITE);
        }
      });

    in_view_white_on_black.setBackgroundColor(
      ReaderColorScheme.SCHEME_WHITE_ON_BLACK.getBackgroundColor());
    in_view_white_on_black.setTextColor(
      ReaderColorScheme.SCHEME_WHITE_ON_BLACK.getForegroundColor());
    in_view_white_on_black.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          settings.setColorScheme(ReaderColorScheme.SCHEME_WHITE_ON_BLACK);
        }
      });

    in_view_black_on_beige.setBackgroundColor(
      ReaderColorScheme.SCHEME_BLACK_ON_BEIGE.getBackgroundColor());
    in_view_black_on_beige.setTextColor(
      ReaderColorScheme.SCHEME_BLACK_ON_BEIGE.getForegroundColor());
    in_view_black_on_beige.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          settings.setColorScheme(ReaderColorScheme.SCHEME_BLACK_ON_BEIGE);
        }
      });

    in_view_text_larger.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          if (settings.getFontScale() < 200) {
            settings.setFontScale(settings.getFontScale() + 25.0f);
          }
        }
      });

    in_view_text_smaller.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          if (settings.getFontScale() > 75) {
            settings.setFontScale(settings.getFontScale() - 25.0f);
          }
        }
      });

    /**
     * Configure brightness controller.
     */

    final int brightness = this.getScreenBrightness();
    in_view_brightness.setProgress(brightness);
    in_view_brightness.setOnSeekBarChangeListener(
      new OnSeekBarChangeListener()
      {
        @Override public void onProgressChanged(
          final @Nullable SeekBar bar,
          final int progress,
          final boolean from_user)
        {
         ReaderSettingsDialog.this.setScreenBrightness(progress);
        }

        @Override public void onStartTrackingTouch(
          final @Nullable SeekBar bar)
        {
          // Nothing
        }

        @Override public void onStopTrackingTouch(
          final @Nullable SeekBar bar)
        {
          // Nothing
        }
      });

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }

    return layout;
  }

  // Change current screen brightness
  private void setScreenBrightness(final int brightness_value) {

    // Make sure brightness value is between 0 to 255
    if (brightness_value >= 0 && brightness_value <= 255) {
      Settings.System.putInt(
        this.getActivity().getApplicationContext().getContentResolver(),
        Settings.System.SCREEN_BRIGHTNESS,
        brightness_value
      );
    }
  }

  // Get current screen brightness
  protected int getScreenBrightness() {

    final int brightness_value = Settings.System.getInt(
      this.getActivity().getApplicationContext().getContentResolver(),
      Settings.System.SCREEN_BRIGHTNESS,
      0
    );
    return brightness_value;
  }

  @Override public void onResume()
  {
    super.onResume();

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final Window window = this.getDialog().getWindow();
    window.setLayout(
      (int) rs.screenDPToPixels(300),
      android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    window.setGravity(Gravity.CENTER);
  }
}
