package org.librarysimplified.r2.demo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import org.librarysimplified.r2.api.SR2PageNumberingMode
import org.librarysimplified.r2.api.SR2ScrollingMode
import org.librarysimplified.r2.views.SR2ReaderModel

class DemoFileSelectionFragment : Fragment(R.layout.demo_file_selection) {

  private lateinit var demoLoaderBrowseButton: Button
  private lateinit var demoLoaderPerChapterPageNumbering: CheckBox
  private lateinit var demoLoaderScrollMode: CheckBox

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val layout =
      inflater.inflate(R.layout.demo_file_selection, container, false)

    this.demoLoaderBrowseButton =
      layout.findViewById(R.id.demoBrowseFileButton)
    this.demoLoaderScrollMode =
      layout.findViewById(R.id.demoScrollMode)
    this.demoLoaderPerChapterPageNumbering =
      layout.findViewById(R.id.demoPerChapterPageNumbering)

    this.demoLoaderBrowseButton.setOnClickListener {
      this.startDocumentPickerForResult()
    }

    this.demoLoaderScrollMode.setOnCheckedChangeListener { _, isChecked ->
      DemoModel.scrollMode = when (isChecked) {
        true -> SR2ScrollingMode.SCROLLING_MODE_CONTINUOUS
        false -> SR2ScrollingMode.SCROLLING_MODE_PAGINATED
      }
    }

    this.demoLoaderPerChapterPageNumbering.setOnCheckedChangeListener { _, isChecked ->
      SR2ReaderModel.perChapterNumbering = when (isChecked) {
        true -> SR2PageNumberingMode.PER_CHAPTER
        false -> SR2PageNumberingMode.WHOLE_BOOK
      }
    }

    SR2ReaderModel.perChapterNumbering = SR2PageNumberingMode.WHOLE_BOOK
    return layout
  }

  /**
   * Present the native document picker and prompt the user to select an EPUB.
   */

  private fun startDocumentPickerForResult() {
    val pickIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      this.type = "*/*"
      this.addCategory(Intent.CATEGORY_OPENABLE)

      // Filter by MIME type; Android versions prior to Marshmallow don't seem
      // to understand the 'application/epub+zip' MIME type.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this.putExtra(
          Intent.EXTRA_MIME_TYPES,
          arrayOf("application/epub+zip"),
        )
      }
    }

    this.requireActivity().startActivityForResult(pickIntent, DemoActivity.PICK_DOCUMENT)
  }
}
