package org.librarysimplified.r2.demo

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.zaphlabs.filechooser.KnotFileChooser
import com.zaphlabs.filechooser.Sorter
import java.io.File

/**
 * A fragment that allows for selecting a book from the device filesystem.
 */

class DemoIntroFragment : Fragment() {

  private var file: File? = null
  private lateinit var choose: Button
  private lateinit var path: TextView
  private lateinit var read: Button

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.demo_intro, container, false)

    this.path =
      layout.findViewById(R.id.fileChooserPath)
    this.choose =
      layout.findViewById(R.id.fileChooseButton)
    this.read =
      layout.findViewById(R.id.fileReadButton)

    this.read.isEnabled = false
    return layout
  }

  override fun onStart() {
    super.onStart()

    this.read.isEnabled = this.file != null

    this.choose.setOnClickListener {
      KnotFileChooser(
        context = this.requireContext(),
        allowBrowsing = true,
        allowCreateFolder = false,
        allowMultipleFiles = false,
        allowSelectFolder = false,
        minSelectedFiles = 1,
        maxSelectedFiles = 1,
        showFiles = true,
        showFoldersFirst = true,
        showFolders = true,
        showHiddenFiles = true,
        initialFolder = Environment.getExternalStorageDirectory(),
        restoreFolder = false,
        cancelable = true
      )
        .title(R.string.fileSelectTitle)
        .sorter(Sorter.ByNameInAscendingOrder)
        .onSelectedFilesListener { selectedFiles ->
          if (selectedFiles.isNotEmpty()) {
            this.file = selectedFiles[0]
          }
          this.read.isEnabled = this.file != null
        }
        .show()
    }

    this.read.setOnClickListener {
      val currentFile = this.file
      if (currentFile != null) {
        (this.requireActivity() as DemoNavigationControllerType)
          .openReader(currentFile)
      }
    }
  }
}