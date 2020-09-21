package org.nypl.simplified.viewer.epub.readium1

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.io7m.jnull.Nullable
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.api.ReaderColorScheme
import org.nypl.simplified.reader.api.ReaderFontSelection
import org.nypl.simplified.reader.api.ReaderPreferences
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory

/**
 * The reader settings dialog, allowing the selection of colors and fonts.
 */

class ReaderSettingsDialog : DialogFragment() {

  private val logger = LoggerFactory.getLogger(ReaderSettingsDialog::class.java)

  private lateinit var profiles: ProfilesControllerType
  private lateinit var readerPreferencesBuilder: ReaderPreferences.Builder
  private lateinit var screen: ScreenSizeInformationType
  private lateinit var uiThread: UIThreadServiceType
  private lateinit var viewBlackOnBeige: TextView
  private lateinit var viewBlackOnWhite: TextView
  private lateinit var viewBrightness: SeekBar
  private lateinit var viewCloseButton: Button
  private lateinit var viewFontOpenDyslexic: TextView
  private lateinit var viewFontSans: TextView
  private lateinit var viewFontSerif: TextView
  private lateinit var viewTextLarger: TextView
  private lateinit var viewTextSmaller: TextView
  private lateinit var viewWhiteOnBlack: TextView
  private var profileEvents: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val services = Services.serviceDirectory()

    this.profiles =
      services.requireService(ProfilesControllerType::class.java)
    this.screen =
      services.requireService(ScreenSizeInformationType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?
  ): View {
    val layout =
      inflater.inflate(R.layout.reader_settings, container, false) as LinearLayout

    this.viewFontSerif =
      layout.findViewById(R.id.reader_settings_font_serif)
    this.viewFontSans =
      layout.findViewById(R.id.reader_settings_font_sans)
    this.viewFontOpenDyslexic =
      layout.findViewById(R.id.reader_settings_font_open_dyslexic)

    this.viewBlackOnWhite =
      layout.findViewById(R.id.reader_settings_black_on_white)
    this.viewWhiteOnBlack =
      layout.findViewById(R.id.reader_settings_white_on_black)
    this.viewBlackOnBeige =
      layout.findViewById(R.id.reader_settings_black_on_beige)

    this.viewTextSmaller =
      layout.findViewById(R.id.reader_settings_text_smaller)
    this.viewTextLarger =
      layout.findViewById(R.id.reader_settings_text_larger)

    this.viewBrightness =
      layout.findViewById(R.id.reader_settings_brightness)
    this.viewCloseButton =
      layout.findViewById(R.id.reader_settings_close)

    try {
      val readerPreferences =
        this.profiles
          .profileCurrent()
          .preferences()
          .readerPreferences

      this.readerPreferencesBuilder = readerPreferences.toBuilder()
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    }

    /*
     * Configure the settings buttons.
     */

    this.viewFontSerif.setOnClickListener {
      this.readerPreferencesBuilder.setFontFamily(ReaderFontSelection.READER_FONT_SERIF)
      this.updatePreferences(this.readerPreferencesBuilder.build())
    }
    this.viewFontSans.setOnClickListener {
      this.readerPreferencesBuilder.setFontFamily(ReaderFontSelection.READER_FONT_SANS_SERIF)
      this.updatePreferences(this.readerPreferencesBuilder.build())
    }
    this.viewFontOpenDyslexic.setOnClickListener {
      this.readerPreferencesBuilder.setFontFamily(ReaderFontSelection.READER_FONT_OPEN_DYSLEXIC)
      this.updatePreferences(this.readerPreferencesBuilder.build())
    }

    val openDyslexic =
      Typeface.createFromAsset(this.requireActivity().assets, "OpenDyslexic3-Regular.ttf")

    this.viewFontOpenDyslexic.typeface = openDyslexic

    this.viewBlackOnWhite.setOnClickListener {
      this.readerPreferencesBuilder.setColorScheme(ReaderColorScheme.SCHEME_BLACK_ON_WHITE)
      this.updatePreferences(this.readerPreferencesBuilder.build())
    }

    this.viewWhiteOnBlack.setOnClickListener {
      this.readerPreferencesBuilder.setColorScheme(ReaderColorScheme.SCHEME_WHITE_ON_BLACK)
      this.updatePreferences(this.readerPreferencesBuilder.build())
    }

    this.viewBlackOnBeige.setOnClickListener {
      this.readerPreferencesBuilder.setColorScheme(ReaderColorScheme.SCHEME_BLACK_ON_BEIGE)
      this.updatePreferences(this.readerPreferencesBuilder.build())
    }

    this.viewTextLarger.setOnClickListener {
      if (this.readerPreferencesBuilder.fontScale() < 250) {
        this.readerPreferencesBuilder.setFontScale(this.readerPreferencesBuilder.fontScale() + 25.0f)
        this.updatePreferences(this.readerPreferencesBuilder.build())
      }
    }

    this.viewTextSmaller.setOnClickListener {
      if (this.readerPreferencesBuilder.fontScale() > 75) {
        this.readerPreferencesBuilder.setFontScale(this.readerPreferencesBuilder.fontScale() - 25.0f)
        this.updatePreferences(this.readerPreferencesBuilder.build())
      }
    }

    this.viewCloseButton.setOnClickListener { this.dismiss() }

    /*
     * Configure brightness controller.
     */

    this.viewBrightness.progress = (this.readerPreferencesBuilder.brightness() * 100).toInt()
    this.viewBrightness.setOnSeekBarChangeListener(
      object : OnSeekBarChangeListener {

        private var bright = 0.5

        override fun onProgressChanged(
          @Nullable bar: SeekBar,
          progress: Int,
          from_user: Boolean
        ) {
          this.bright = progress / 100.0
        }

        override fun onStartTrackingTouch(@Nullable bar: SeekBar) {
        }

        override fun onStopTrackingTouch(@Nullable bar: SeekBar) {
          this@ReaderSettingsDialog.updateBrightness(this.bright.toFloat())
        }
      })

    this.onReaderPreferencesChanged(this.readerPreferencesBuilder.build())
    return layout
  }

  override fun onStart() {
    super.onStart()

    this.profileEvents =
      this.profiles
        .profileEvents()
        .subscribe(this::onProfileEvent)

    this.dialog!!.window!!.let { window ->
      window.setLayout(
        this.screen.dpToPixels(300).toInt(),
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      window.setGravity(Gravity.CENTER)
    }
    this.dialog!!.setCanceledOnTouchOutside(true)
  }

  override fun onStop() {
    super.onStop()

    this.profileEvents?.dispose()
  }

  private fun onProfileEvent(event: ProfileEvent) {
    if (event is ProfileUpdated.Succeeded) {
      if (event.oldDescription.preferences.readerPreferences != event.newDescription.preferences.readerPreferences) {
        this.onReaderPreferencesChanged(event.newDescription.preferences.readerPreferences)
      }
    }
  }

  private fun onReaderPreferencesChanged(newPreferences: ReaderPreferences) {
    this.logger.debug("reader preferences changed")

    val colorScheme = newPreferences.colorScheme()
    val fontFamily = newPreferences.fontFamily()

    this.uiThread.runOnUIThread {
      this.configureViewsForColorScheme(colorScheme)
      this.configureViewsForFont(fontFamily)
    }
  }

  private fun configureViewsForFont(fontFamily: ReaderFontSelection) {
    when (fontFamily) {
      ReaderFontSelection.READER_FONT_SANS_SERIF -> {
        this.viewFontOpenDyslexic.setBackgroundResource(R.drawable.reader_settings_font)
        this.viewFontSans.setBackgroundResource(R.drawable.reader_settings_font_active)
        this.viewFontSerif.setBackgroundResource(R.drawable.reader_settings_font)
      }
      ReaderFontSelection.READER_FONT_OPEN_DYSLEXIC -> {
        this.viewFontOpenDyslexic.setBackgroundResource(R.drawable.reader_settings_font_active)
        this.viewFontSans.setBackgroundResource(R.drawable.reader_settings_font)
        this.viewFontSerif.setBackgroundResource(R.drawable.reader_settings_font)
      }
      ReaderFontSelection.READER_FONT_SERIF -> {
        this.viewFontOpenDyslexic.setBackgroundResource(R.drawable.reader_settings_font)
        this.viewFontSans.setBackgroundResource(R.drawable.reader_settings_font)
        this.viewFontSerif.setBackgroundResource(R.drawable.reader_settings_font_active)
      }
    }
  }

  private fun configureViewsForColorScheme(colorScheme: ReaderColorScheme) {
    when (colorScheme) {
      ReaderColorScheme.SCHEME_BLACK_ON_BEIGE -> {
        this.viewBlackOnBeige.setBackgroundResource(R.drawable.reader_settings_black_on_beige_active)
        this.viewBlackOnWhite.setBackgroundResource(R.drawable.reader_settings_black_on_white)
        this.viewWhiteOnBlack.setBackgroundResource(R.drawable.reader_settings_white_on_black)
      }
      ReaderColorScheme.SCHEME_BLACK_ON_WHITE -> {
        this.viewBlackOnBeige.setBackgroundResource(R.drawable.reader_settings_black_on_beige)
        this.viewBlackOnWhite.setBackgroundResource(R.drawable.reader_settings_black_on_white_active)
        this.viewWhiteOnBlack.setBackgroundResource(R.drawable.reader_settings_white_on_black)
      }
      ReaderColorScheme.SCHEME_WHITE_ON_BLACK -> {
        this.viewBlackOnBeige.setBackgroundResource(R.drawable.reader_settings_black_on_beige)
        this.viewBlackOnWhite.setBackgroundResource(R.drawable.reader_settings_black_on_white)
        this.viewWhiteOnBlack.setBackgroundResource(R.drawable.reader_settings_white_on_black_active)
      }
    }
  }

  private fun updateBrightness(backLightValue: Float) {
    val activity = this.requireActivity()
    val layoutParams = activity.window.attributes
    layoutParams.screenBrightness = backLightValue
    activity.window.attributes = layoutParams

    this.readerPreferencesBuilder.setBrightness(backLightValue.toDouble())
    this.updatePreferences(this.readerPreferencesBuilder.build())
  }

  private fun updatePreferences(prefs: ReaderPreferences) {
    this.profiles.profileUpdate { description ->
      description.copy(preferences = description.preferences.copy(readerPreferences = prefs))
    }
  }

  companion object {

    fun create(): ReaderSettingsDialog {
      return ReaderSettingsDialog()
    }
  }
}
