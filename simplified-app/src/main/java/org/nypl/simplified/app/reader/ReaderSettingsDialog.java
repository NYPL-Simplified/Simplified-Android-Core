package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class ReaderSettingsDialog extends DialogFragment
{
  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedBookDialog);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;

    final Resources rr = NullCheck.notNull(this.getResources());
    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.reader_settings,
        container,
        false));

    final ViewGroup in_view_container =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.reader_settings_container));
    final TextView in_view_black_on_white =
      NullCheck.notNull((TextView) layout
        .findViewById(R.id.reader_settings_black_on_white));
    final TextView in_view_white_on_black =
      NullCheck.notNull((TextView) layout
        .findViewById(R.id.reader_settings_white_on_black));
    final TextView in_view_black_on_beige =
      NullCheck.notNull((TextView) layout
        .findViewById(R.id.reader_settings_black_on_beige));
    final TextView in_view_text_smaller =
      NullCheck.notNull((TextView) layout
        .findViewById(R.id.reader_settings_text_smaller));
    final TextView in_view_text_larger =
      NullCheck.notNull((TextView) layout
        .findViewById(R.id.reader_settings_text_larger));
    final SeekBar in_view_brightness =
      NullCheck.notNull((SeekBar) layout
        .findViewById(R.id.reader_settings_brightness));

    /**
     * Configure the settings buttons.
     */

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();
    final ReaderSettingsType settings = rs.getSettings();

    in_view_black_on_white
      .setBackgroundColor(ReaderColorScheme.SCHEME_BLACK_ON_WHITE
        .getBackgroundColor());
    in_view_black_on_white
      .setTextColor(ReaderColorScheme.SCHEME_BLACK_ON_WHITE
        .getForegroundColor());
    in_view_black_on_white.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        settings.setColorScheme(ReaderColorScheme.SCHEME_BLACK_ON_WHITE);
      }
    });

    in_view_white_on_black
      .setBackgroundColor(ReaderColorScheme.SCHEME_WHITE_ON_BLACK
        .getBackgroundColor());
    in_view_white_on_black
      .setTextColor(ReaderColorScheme.SCHEME_WHITE_ON_BLACK
        .getForegroundColor());
    in_view_white_on_black.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        settings.setColorScheme(ReaderColorScheme.SCHEME_WHITE_ON_BLACK);
      }
    });

    in_view_black_on_beige
      .setBackgroundColor(ReaderColorScheme.SCHEME_BLACK_ON_BEIGE
        .getBackgroundColor());
    in_view_black_on_beige
      .setTextColor(ReaderColorScheme.SCHEME_BLACK_ON_BEIGE
        .getForegroundColor());
    in_view_black_on_beige.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        settings.setColorScheme(ReaderColorScheme.SCHEME_BLACK_ON_BEIGE);
      }
    });

    in_view_text_larger.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        settings.setFontScale(settings.getFontScale() + 25.0f);
      }
    });

    in_view_text_smaller.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        settings.setFontScale(settings.getFontScale() - 25.0f);
      }
    });

    /**
     * Configure brightness controller.
     */

    final Activity activity = this.getActivity();
    final Window window = activity.getWindow();
    in_view_brightness
      .setProgress((int) (window.getAttributes().screenBrightness * 100));
    in_view_brightness
      .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        @Override public void onStopTrackingTouch(
          final @Nullable SeekBar bar)
        {
          // Nothing
        }

        @Override public void onStartTrackingTouch(
          final @Nullable SeekBar bar)
        {
          // Nothing
        }

        @Override public void onProgressChanged(
          final @Nullable SeekBar bar,
          final int progress,
          final boolean fromUser)
        {
          final WindowManager.LayoutParams params = window.getAttributes();
          params.screenBrightness = progress / 100.0f;
          window.setAttributes(params);
        }
      });

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }

    return layout;
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
