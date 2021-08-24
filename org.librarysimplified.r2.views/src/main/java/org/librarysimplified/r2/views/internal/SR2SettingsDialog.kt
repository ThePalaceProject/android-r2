package org.librarysimplified.r2.views.internal

import android.content.Context
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
import org.librarysimplified.r2.api.SR2ColorScheme
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Font
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_DISABLED
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_ENABLED
import org.librarysimplified.r2.api.SR2Theme
import org.librarysimplified.r2.views.R

internal class SR2SettingsDialog private constructor() {
  companion object {
    private fun updateTheme(
      controller: SR2ControllerType,
      updater: (SR2Theme) -> SR2Theme
    ) {
      controller.submitCommand(SR2Command.ThemeSet(updater.invoke(controller.themeNow())))
    }

    fun create(
      brightness: SR2BrightnessServiceType,
      context: Context,
      controller: SR2ControllerType
    ): SR2SettingsDialog {
      val dialog =
        AlertDialog.Builder(context)
          .setView(R.layout.sr2_settings)
          .create()

      dialog.show()

      val setFontSans =
        dialog.findViewById<View>(R.id.setFontSans)!!
      val setFontSerif =
        dialog.findViewById<View>(R.id.setFontSerif)!!
      val setFontDyslexic =
        dialog.findViewById<View>(R.id.setFontDyslexic)!!
      val setFontDyslexicText =
        dialog.findViewById<TextView>(R.id.setFontDyslexicText)!!
      val setThemeLight =
        dialog.findViewById<View>(R.id.setThemeLight)!!
      val setThemeDark =
        dialog.findViewById<View>(R.id.setThemeDark)!!
      val setThemeSepia =
        dialog.findViewById<View>(R.id.setThemeSepia)!!
      val setTextSmaller =
        dialog.findViewById<View>(R.id.setTextSmaller)!!
      val setTextReset =
        dialog.findViewById<View>(R.id.setTextSizeReset)!!
      val setTextLarger =
        dialog.findViewById<View>(R.id.setTextLarger)!!
      val setBrightness =
        dialog.findViewById<SeekBar>(R.id.setBrightness)!!
      val publisherCSS =
        dialog.findViewById<SwitchCompat>(R.id.sr2_publisher_css_switch)!!

      val openDyslexic = ResourcesCompat.getFont(context, R.font.open_dyslexic)
      setFontDyslexicText.typeface = openDyslexic

      setFontSans.setOnClickListener {
        this.updateTheme(controller) { it.copy(font = SR2Font.FONT_SANS) }
      }
      setFontSerif.setOnClickListener {
        this.updateTheme(controller) { it.copy(font = SR2Font.FONT_SERIF) }
      }
      setFontDyslexic.setOnClickListener {
        this.updateTheme(controller) { it.copy(font = SR2Font.FONT_OPEN_DYSLEXIC) }
      }
      setThemeLight.setOnClickListener {
        this.updateTheme(controller) { it.copy(colorScheme = SR2ColorScheme.DARK_TEXT_LIGHT_BACKGROUND) }
      }
      setThemeDark.setOnClickListener {
        this.updateTheme(controller) { it.copy(colorScheme = SR2ColorScheme.LIGHT_TEXT_DARK_BACKGROUND) }
      }
      setThemeSepia.setOnClickListener {
        this.updateTheme(controller) { it.copy(colorScheme = SR2ColorScheme.DARK_TEXT_ON_SEPIA) }
      }
      setTextLarger.setOnClickListener {
        this.updateTheme(controller) { it.copy(textSize = SR2Theme.sizeConstrain(it.textSize + 0.1)) }
      }
      setTextReset.setOnClickListener {
        this.updateTheme(controller) { it.copy(textSize = 1.0) }
      }
      setTextSmaller.setOnClickListener {
        this.updateTheme(controller) { it.copy(textSize = SR2Theme.sizeConstrain(it.textSize - 0.1)) }
      }

      publisherCSS.isChecked =
        when (controller.themeNow().publisherCSS) {
          SR2_PUBLISHER_DEFAULT_CSS_ENABLED -> true
          SR2_PUBLISHER_DEFAULT_CSS_DISABLED -> false
        }

      publisherCSS.setOnCheckedChangeListener { buttonView, isChecked ->
        this.updateTheme(controller) {
          it.copy(
            publisherCSS = if (isChecked) {
              SR2_PUBLISHER_DEFAULT_CSS_ENABLED
            } else {
              SR2_PUBLISHER_DEFAULT_CSS_DISABLED
            }
          )
        }
      }

      val brightnessInitial = brightness.brightness()
      setBrightness.progress = (brightnessInitial * 100.0).toInt()
      setBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        var bright = brightnessInitial
        override fun onProgressChanged(
          seekBar: SeekBar,
          progress: Int,
          fromUser: Boolean
        ) {
          this.bright = progress / 100.0
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
          brightness.setBrightness(this.bright)
        }
      })

      return SR2SettingsDialog()
    }
  }
}
