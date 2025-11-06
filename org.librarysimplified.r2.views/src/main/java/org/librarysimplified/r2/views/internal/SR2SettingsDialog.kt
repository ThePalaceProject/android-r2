package org.librarysimplified.r2.views.internal

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.r2.api.SR2ColorScheme
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Font
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_DISABLED
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_ENABLED
import org.librarysimplified.r2.api.SR2Theme
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.SR2ReaderModel

internal class SR2SettingsDialog private constructor() {

  enum class FontSelectionTab {
    SANS,
    SERIF,
    DYSLEXIC,
    PUB,
    ;

    fun toThemeUpdater(): (SR2Theme) -> SR2Theme = {
      val font = when (this) {
        SANS -> SR2Font.FONT_SANS
        SERIF -> SR2Font.FONT_SERIF
        DYSLEXIC -> SR2Font.FONT_OPEN_DYSLEXIC
        PUB -> it.font
      }
      val pubDefault = when (this) {
        PUB -> SR2_PUBLISHER_DEFAULT_CSS_ENABLED
        else -> SR2_PUBLISHER_DEFAULT_CSS_DISABLED
      }
      it.copy(font = font, publisherCSS = pubDefault)
    }

    companion object {
      fun fromTheme(theme: SR2Theme): FontSelectionTab =
        if (theme.publisherCSS == SR2_PUBLISHER_DEFAULT_CSS_ENABLED) {
          PUB
        } else {
          when (theme.font) {
            SR2Font.FONT_SERIF -> SERIF
            SR2Font.FONT_SANS -> SANS
            SR2Font.FONT_OPEN_DYSLEXIC -> DYSLEXIC
          }
        }
    }
  }

  companion object {

    private var isOpen = false

    fun isOpen(): Boolean {
      return this.isOpen
    }

    private fun updateTheme(
      updater: (SR2Theme) -> SR2Theme,
    ) {
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
          }
          .create()

      dialog.show()
      isOpen = true

      val inflater = LayoutInflater.from(context)
      val setFontTabs = dialog.findViewById<TabLayout>(R.id.setFontTabs)!!

      val setFontSans = inflater.inflate(R.layout.sr2_settings_fonts_sans, null)!!
      setFontTabs.getTabAt(0)!!.customView = setFontSans

      val setFontSerif = inflater.inflate(R.layout.sr2_settings_fonts_serif, null)!!
      setFontTabs.getTabAt(1)!!.customView = setFontSerif

      val setFontDyslexic = inflater.inflate(R.layout.sr2_settings_fonts_dyslexic, null)!!
      setFontTabs.getTabAt(2)!!.customView = setFontDyslexic
      val setFontDyslexicText = setFontDyslexic.findViewById<TextView>(R.id.setFontDyslexicText)!!
      val openDyslexic = ResourcesCompat.getFont(context, R.font.open_dyslexic)
      setFontDyslexicText.typeface = openDyslexic

      val setFontPub = inflater.inflate(R.layout.sr2_settings_fonts_pub, null)!!
      setFontTabs.getTabAt(3)!!.customView = setFontPub
      val setFontDetail = dialog.findViewById<View>(R.id.setFontDetail)!!

      setFontTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
          val selectedTab = FontSelectionTab.values()[tab.position]
          val updater = selectedTab.toThemeUpdater()
          this@Companion.updateTheme(updater)
          val showDetail = selectedTab == FontSelectionTab.PUB
          setFontDetail.visibility = if (showDetail) View.VISIBLE else View.GONE
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
          // Do nothing
        }

        override fun onTabReselected(tab: TabLayout.Tab) {
          // Do nothing
        }
      })

      val currentTabIndex =
        FontSelectionTab.fromTheme(SR2ReaderModel.theme()).ordinal

      setFontTabs.selectTab(setFontTabs.getTabAt(currentTabIndex))

      val setThemeLight =
        dialog.findViewById<View>(R.id.setThemeLight)!!
      val setThemeDark =
        dialog.findViewById<View>(R.id.setThemeDark)!!
      val setThemeSepia =
        dialog.findViewById<View>(R.id.setThemeSepia)!!

      setThemeLight.setOnClickListener {
        this.updateTheme { it.copy(colorScheme = SR2ColorScheme.DARK_TEXT_LIGHT_BACKGROUND) }
      }
      setThemeDark.setOnClickListener {
        this.updateTheme { it.copy(colorScheme = SR2ColorScheme.LIGHT_TEXT_DARK_BACKGROUND) }
      }
      setThemeSepia.setOnClickListener {
        this.updateTheme { it.copy(colorScheme = SR2ColorScheme.DARK_TEXT_ON_SEPIA) }
      }

      val setTextSmaller =
        dialog.findViewById<View>(R.id.setTextSmaller)!!
      val setTextReset =
        dialog.findViewById<View>(R.id.setTextSizeReset)!!
      val setTextLarger =
        dialog.findViewById<View>(R.id.setTextLarger)!!
      val setBrightness =
        dialog.findViewById<SeekBar>(R.id.setBrightness)!!

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
      setBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
      })

      return SR2SettingsDialog()
    }
  }
}
