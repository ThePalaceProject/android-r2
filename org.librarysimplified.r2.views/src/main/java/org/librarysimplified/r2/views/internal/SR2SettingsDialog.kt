package org.librarysimplified.r2.views.internal

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.r2.api.SR2ColorScheme
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Font
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_DISABLED
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_ENABLED
import org.librarysimplified.r2.api.SR2Theme
import org.librarysimplified.r2.api.SR2UISettings
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.SR2ReaderModel

internal class SR2SettingsDialog private constructor() {
  companion object {
    private var isOpen = false

    fun isOpen(): Boolean = this.isOpen

    private fun updateTheme(updater: (SR2Theme) -> SR2Theme) {
      SR2ReaderModel.submitCommand(SR2Command.ThemeSet(updater.invoke(SR2ReaderModel.theme())))
    }

    fun create(
      brightness: SR2BrightnessServiceType,
      context: Context,
    ): SR2SettingsDialog {
      val eventSubscriptions = CompositeDisposable()

      val dialog =
        MaterialAlertDialogBuilder(context)
          .setView(R.layout.sr2_settings)
          .setOnDismissListener {
            isOpen = false
            eventSubscriptions.dispose()
          }.create()

      dialog.show()
      isOpen = true

      val setFontSans =
        dialog.findViewById<View>(R.id.sr2_set_font_sans)!!
      val setFontSerif =
        dialog.findViewById<View>(R.id.sr2_set_font_serif)!!
      val setFontDyslexic =
        dialog.findViewById<View>(R.id.sr2_set_font_dyslexic)!!
      val setFontPub =
        dialog.findViewById<View>(R.id.sr2_set_font_publisher)!!
      val publisherExplanation =
        dialog.findViewById<ViewGroup>(R.id.sr2_publisher_explanation)!!
      val pageButtonsOff =
        dialog.findViewById<View>(R.id.sr2_set_page_buttons_off)!!
      val pageButtonsSmall =
        dialog.findViewById<View>(R.id.sr2_set_page_buttons_small)!!
      val pageButtonsMedium =
        dialog.findViewById<View>(R.id.sr2_set_page_buttons_medium)!!
      val pageButtonsLarge =
        dialog.findViewById<View>(R.id.sr2_set_page_buttons_large)!!

      publisherExplanation.visibility = View.GONE

      pageButtonsOff.setOnClickListener {
        this.updateSettings { f ->
          f.copy(pageButtonWidth = null)
        }
      }
      pageButtonsSmall.setOnClickListener {
        this.updateSettings { f ->
          f.copy(pageButtonWidth = SR2UISettings.pageButtonWidthSmall)
        }
      }
      pageButtonsMedium.setOnClickListener {
        this.updateSettings { f ->
          f.copy(pageButtonWidth = SR2UISettings.pageButtonWidthMedium)
        }
      }
      pageButtonsLarge.setOnClickListener {
        this.updateSettings { f ->
          f.copy(pageButtonWidth = SR2UISettings.pageButtonWidthLarge)
        }
      }

      setFontSans.setOnClickListener {
        publisherExplanation.visibility = View.GONE
        this.updateTheme {
          it.copy(
            font = SR2Font.FONT_SANS,
            publisherCSS = SR2_PUBLISHER_DEFAULT_CSS_DISABLED
          )
        }
      }
      setFontSerif.setOnClickListener {
        publisherExplanation.visibility = View.GONE
        this.updateTheme {
          it.copy(
            font = SR2Font.FONT_SERIF,
            publisherCSS = SR2_PUBLISHER_DEFAULT_CSS_DISABLED
          )
        }
      }
      setFontDyslexic.setOnClickListener {
        publisherExplanation.visibility = View.GONE
        this.updateTheme {
          it.copy(
            font = SR2Font.FONT_OPEN_DYSLEXIC,
            publisherCSS = SR2_PUBLISHER_DEFAULT_CSS_DISABLED
          )
        }
      }
      setFontPub.setOnClickListener {
        publisherExplanation.visibility = View.VISIBLE
        this.updateTheme { it.copy(publisherCSS = SR2_PUBLISHER_DEFAULT_CSS_ENABLED) }
      }

      val setThemeBlackOnWhite =
        dialog.findViewById<View>(R.id.sr2_set_black_on_white)!!
      val setThemeWhiteOnBlack =
        dialog.findViewById<View>(R.id.sr2_set_white_on_black)!!
      val setThemeBlackOnSepia =
        dialog.findViewById<View>(R.id.sr2_set_black_on_sepia)!!

      setThemeBlackOnWhite.setOnClickListener {
        this.updateTheme { it.copy(colorScheme = SR2ColorScheme.DARK_TEXT_LIGHT_BACKGROUND) }
      }
      setThemeWhiteOnBlack.setOnClickListener {
        this.updateTheme { it.copy(colorScheme = SR2ColorScheme.LIGHT_TEXT_DARK_BACKGROUND) }
      }
      setThemeBlackOnSepia.setOnClickListener {
        this.updateTheme { it.copy(colorScheme = SR2ColorScheme.DARK_TEXT_ON_SEPIA) }
      }

      val setTextSmaller =
        dialog.findViewById<View>(R.id.sr2_set_text_smaller)!!
      val setTextReset =
        dialog.findViewById<View>(R.id.sr2_set_text_reset)!!
      val setTextLarger =
        dialog.findViewById<View>(R.id.sr2_set_text_larger)!!
      val setBrightness =
        dialog.findViewById<SeekBar>(R.id.setBrightness)!!
      val dismiss =
        dialog.findViewById<Button>(R.id.dismiss)!!

      dismiss.setOnClickListener {
        dialog.dismiss()
      }

      setTextLarger.setOnClickListener {
        this.updateTheme { it.copy(textSize = SR2Theme.sizeConstrain(it.textSize + 0.1)) }
      }
      setTextReset.setOnClickListener {
        this.updateTheme { it.copy(textSize = 1.0) }
      }
      setTextSmaller.setOnClickListener {
        this.updateTheme { it.copy(textSize = SR2Theme.sizeConstrain(it.textSize - 0.1)) }
      }

      fun updateSetText() {
        val themeNow = SR2ReaderModel.theme()
        setTextSmaller.isEnabled = !themeNow.isTextSizeMinimized
        setTextLarger.isEnabled = !themeNow.isTextSizeMaximized
      }

      eventSubscriptions.add(
        SR2ReaderModel.controllerEvents
          .ofType(SR2Event.SR2ThemeChanged::class.java)
          .subscribe { event -> updateSetText() },
      )

      updateSetText()

      val brightnessInitial = brightness.brightness()
      setBrightness.progress = (brightnessInitial * 100.0).toInt()
      setBrightness.setOnSeekBarChangeListener(
        object : SeekBar.OnSeekBarChangeListener {
          var bright = brightnessInitial

          override fun onProgressChanged(
            seekBar: SeekBar,
            progress: Int,
            fromUser: Boolean,
          ) {
            this.bright = progress / 100.0
          }

          override fun onStartTrackingTouch(seekBar: SeekBar) {
          }

          override fun onStopTrackingTouch(seekBar: SeekBar) {
            brightness.setBrightness(this.bright)
          }
        },
      )

      return SR2SettingsDialog()
    }

    private fun updateSettings(f: (SR2UISettings) -> SR2UISettings) {
      SR2ReaderModel.submitCommand(SR2Command.UISettingsSet(f.invoke(SR2ReaderModel.uiSettings())))
    }
  }
}
