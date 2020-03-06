package org.librarysimplified.r2.demo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.disposables.Disposable
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.vanilla.SR2Controllers
import org.librarysimplified.r2.vanilla.UIThread
import org.librarysimplified.r2.views.SR2ControllerHostType
import org.librarysimplified.r2.views.SR2ReaderFragment
import org.librarysimplified.r2.views.SR2ReaderFragmentParameters
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

class DemoActivity : AppCompatActivity(), SR2ControllerHostType {

  companion object {
    const val PICK_DOCUMENT = 1001
  }

  private val logger = LoggerFactory.getLogger(DemoActivity::class.java)

  private val ioExecutor =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1) { runnable ->
      val thread = Thread(runnable)
      thread.name = "org.librarysimplified.r2.demo.io"
      thread
    })

  private var epubFile: File? = null
  private var controller: SR2ControllerType? = null
  private var controllerSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState == null) {
      setContentView(R.layout.demo_activity)

      val browseButton = findViewById<Button>(R.id.browse_button)!!
      browseButton.setOnClickListener { startDocumentPickerForResult() }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      PICK_DOCUMENT -> onPickDocumentResult(resultCode, data)
    }
  }

  override fun onResume() {
    super.onResume()
    epubFile?.let { startReader(it) }
  }

  override fun onStop() {
    super.onStop()
    controllerSubscription?.dispose()
  }

  override fun onControllerRequired(): SR2ControllerProviderType {
    return SR2Controllers()
  }

  override fun onControllerBecameAvailable(controller: SR2ControllerType) {
    this.controller = controller

    // Listen for messages from the controller.
    controllerSubscription =
      controller.events.subscribe { event -> onControllerEvent(event) }

    // Navigate to the first chapter or saved reading position.
    controller.submitCommand(SR2Command.OpenChapter(0))
  }

  override fun onControllerWantsIOExecutor(): ListeningExecutorService {
    return ioExecutor
  }

  override fun onNavigationClose() {
    TODO("not implemented")
  }

  override fun onNavigationOpenTableOfContents() {
    TODO("not implemented")
  }

  /**
   * Start the reader with the given EPUB.
   */

  private fun startReader(file: File) {
    val fragment =
      SR2ReaderFragment.create(
        SR2ReaderFragmentParameters(
          bookFile = file
        )
      )

    supportFragmentManager.beginTransaction()
      .replace(R.id.readerContainer, fragment)
      .commit()
  }

  /**
   * Handle incoming messages from the controller.
   */

  private fun onControllerEvent(event: SR2Event) {
    when (event) {
      is SR2Event.SR2Error.SR2ChapterNonexistent -> {
        UIThread.runOnUIThread {
          Toast.makeText(this, "Chapter nonexistent: ${event.chapterIndex}", Toast.LENGTH_SHORT)
            .show()
        }
      }
      is SR2Event.SR2Error.SR2WebViewInaccessible -> {
        UIThread.runOnUIThread {
          Toast.makeText(this, "Web view inaccessible!", Toast.LENGTH_SHORT).show()
        }
      }
      is SR2Event.SR2OnCenterTapped -> {
        UIThread.runOnUIThread {
          Toast.makeText(this, "Center tap!", Toast.LENGTH_SHORT).show()
        }
      }
      is SR2Event.SR2ReadingPositionChanged -> {
        UIThread.runOnUIThread {
          val percent = event.progress * 100.0
          val percentText = String.format("%.2f", percent)
          Toast.makeText(this, "Chapter ${event.chapterIndex}, $percentText%", Toast.LENGTH_SHORT)
            .show()
        }
      }
    }
  }

  /**
   * Handle the result of a pick document intent.
   */

  private fun onPickDocumentResult(resultCode: Int, intent: Intent?) {
    // Assume the user picked a valid EPUB file. In reality, we'd want to verify
    // this is a supported file type.
    if (resultCode == Activity.RESULT_OK) {
      intent?.data?.let { uri ->
        // This copy operation should be done on a worker thread; omitted for brevity.
        epubFile = copyToStorage(uri)
      }
    }
  }

  /**
   * Present the user with an error message.
   */

  private fun showError(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
  }

  /**
   * Copy the book to internal storage; returns null if an error occurs.
   */

  private fun copyToStorage(uri: Uri): File? {
    val file = File(filesDir, "book.epub")
    var ips: InputStream? = null
    var ops: OutputStream? = null

    try {
      ips = contentResolver.openInputStream(uri)
      ops = file.outputStream()
      ips.copyTo(ops)
      return file
    } catch (e: FileNotFoundException) {
      logger.warn("File not found", e)
      showError("File not found")
    } catch (e: IOException) {
      logger.warn("Error copying file", e)
      showError("Error copying file")
    } finally {
      ips?.close()
      ops?.close()
    }
    return null
  }

  /**
   * Present the native document picker and prompt the user to select an EPUB.
   */

  private fun startDocumentPickerForResult() {
    val pickIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      type = "*/*"
      addCategory(Intent.CATEGORY_OPENABLE)

      // Filter by MIME type; Android versions prior to Marshmallow don't seem
      // to understand the 'application/epub+zip' MIME type.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        putExtra(
          Intent.EXTRA_MIME_TYPES,
          arrayOf("application/epub+zip")
        )
      }
    }
    startActivityForResult(pickIntent, PICK_DOCUMENT)
  }
}
