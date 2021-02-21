package org.librarysimplified.r2.vanilla.internal

import android.webkit.WebView
import androidx.annotation.UiThread
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerCommandQueueType
import org.librarysimplified.r2.vanilla.internal.SR2ReaderTheme.DARK
import org.librarysimplified.r2.vanilla.internal.SR2ReaderTheme.DAY
import org.librarysimplified.r2.vanilla.internal.SR2ReaderTheme.LIGHT
import org.librarysimplified.r2.vanilla.internal.SR2ReaderTheme.NIGHT
import org.librarysimplified.r2.vanilla.internal.SR2ReaderTheme.SEPIA
import org.nypl.simplified.ui.thread.api.UIThread
import org.slf4j.LoggerFactory

/**
 * The default implementation of the Javascript API.
 */

internal class SR2JavascriptAPI(
  private val webView: WebView,
  private val commandQueue: SR2ControllerCommandQueueType
) : SR2JavascriptAPIType {

  private val logger =
    LoggerFactory.getLogger(SR2JavascriptAPI::class.java)

  @UiThread
  override fun openPageNext() {
    UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("scrollRight();") {
      this.logger.debug("scrollRight => {}", it)
      when (it) {
        "\"edge\"" -> {
          this.commandQueue.submitCommand(SR2Command.OpenChapterNext)
        }
        else -> {
        }
      }
    }
  }

  @UiThread
  override fun openPagePrevious() {
    UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("scrollLeft();") {
      this.logger.debug("scrollLeft => {}", it)

      when (it) {
        "\"edge\"" -> {
          this.commandQueue.submitCommand(SR2Command.OpenChapterPrevious(atEnd = true))
        }
        else -> {
        }
      }
    }
  }

  @UiThread
  override fun openPageLast() {
    UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("scrollToEnd();") {
      this.logger.debug("scrollToEnd => {}", it)
    }
  }

  override fun setFontFamily(value: String) {
    setUserProperty("fontFamily", value)
    setUserProperty("fontOverride", "readium-font-on")
  }

  override fun setTextSize(value: Int) {
    // Note: The js property name is 'fontSize' not 'textSize'
    setUserProperty("fontSize", "$value%")
  }

  override fun setTextAlign(value: String) {
    setUserProperty("textAlign", value)
  }

  override fun setPageMargin(value: Double) {
    // Note: The js property name is 'pageMargins' plural
    setUserProperty("pageMargins", "$value")
  }

  override fun setLineHeight(value: Double) {
    setUserProperty("lineHeight", "$value")
  }

  override fun setLetterSpacing(value: Double) {
    setUserProperty("letterSpacing", "${value}em")
  }

  override fun setWordSpacing(value: Double) {
    setUserProperty("wordSpacing", "${value}rem")
  }

  override fun setTheme(value: SR2ReaderTheme) {
    when (value) {
      LIGHT, DAY -> {
        setUserProperty("appearance", "readium-default-on")
      }
      DARK, NIGHT -> {
        setUserProperty("appearance", "readium-night-on")
      }
      SEPIA -> {
        setUserProperty("appearance", "readium-sepia-on")
      }
    }
  }

  @UiThread
  override fun setProgression(progress: Double) {
    UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("scrollToPosition($progress);") {
      this.logger.debug("scrollToPosition => {}", it)
    }
  }

  @UiThread
  fun setUserProperty(name: String, value: String) {
    UIThread.checkIsUIThread()

    val script = "setProperty(\"--USER__${name}\", \"${value}\");"
    this.webView.evaluateJavascript(script) {
      this.logger.debug("evaluation result: {}", it)
    }
  }
}
