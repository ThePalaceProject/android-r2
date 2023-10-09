package org.librarysimplified.r2.demo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.reactivex.disposables.Disposable
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreate
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkFailedToBeDeleted
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkTryToDelete
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarksLoaded
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionFailed
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionSucceeded
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionRunningLong
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionStarted
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandSearchResults
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2ChapterNonexistent
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2WebViewInaccessible
import org.librarysimplified.r2.api.SR2Event.SR2ExternalLinkSelected
import org.librarysimplified.r2.api.SR2Event.SR2OnCenterTapped
import org.librarysimplified.r2.api.SR2Event.SR2ReadingPositionChanged
import org.librarysimplified.r2.api.SR2Event.SR2ThemeChanged
import org.librarysimplified.r2.api.SR2PageNumberingMode
import org.librarysimplified.r2.api.SR2ScrollingMode
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.vanilla.SR2Controllers
import org.librarysimplified.r2.views.SR2ControllerReference
import org.librarysimplified.r2.views.SR2ReaderFragment
import org.librarysimplified.r2.views.SR2ReaderFragmentFactory
import org.librarysimplified.r2.views.SR2ReaderParameters
import org.librarysimplified.r2.views.SR2ReaderViewEvent
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookLoadingFailed
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameAvailable
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationClose
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationOpenSearch
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationOpenTOC
import org.librarysimplified.r2.views.SR2ReaderViewModel
import org.librarysimplified.r2.views.SR2ReaderViewModelFactory
import org.librarysimplified.r2.views.SR2TOCFragment
import org.librarysimplified.r2.views.search.SR2SearchFragment
import org.readium.r2.shared.publication.asset.FileAsset
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest

class DemoActivity : AppCompatActivity(R.layout.demo_activity_host) {

  companion object {
    const val PICK_DOCUMENT = 1001
  }

  private val logger =
    LoggerFactory.getLogger(DemoActivity::class.java)

  private lateinit var readerFragmentFactory: SR2ReaderFragmentFactory
  private lateinit var readerParameters: SR2ReaderParameters
  private var controller: SR2ControllerType? = null
  private var controllerSubscription: Disposable? = null
  private var epubFile: File? = null
  private var epubId: String? = null
  private var viewSubscription: Disposable? = null
  private lateinit var scrollMode: CheckBox
  private lateinit var perChapterPageNumbering: CheckBox
  private lateinit var readerFragment: Fragment
  private lateinit var searchFragment: Fragment
  private lateinit var tocFragment: Fragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val toolbar = this.findViewById(R.id.mainToolbar) as Toolbar
    this.setSupportActionBar(toolbar)

    if (savedInstanceState == null) {
      this.setContentView(R.layout.demo_fragment_host)

      val browseButton = this.findViewById<Button>(R.id.browse_button)!!
      browseButton.setOnClickListener { this.startDocumentPickerForResult() }
      this.scrollMode = this.findViewById(R.id.scrollMode)
      this.perChapterPageNumbering = this.findViewById(R.id.perChapterPageNumbering)
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
  ) {
    super.onActivityResult(requestCode, resultCode, data)

    when (requestCode) {
      PICK_DOCUMENT -> this.onPickDocumentResult(resultCode, data)
    }
  }

  override fun onStart() {
    super.onStart()
    this.epubFile?.let(this::startReader)
  }

  override fun onStop() {
    super.onStop()
    this.controllerSubscription?.dispose()
    this.viewSubscription?.dispose()
  }

  override fun onBackPressed() {
    if (this.tocFragment.isVisible) {
      this.tocClose()
    } else if (this.searchFragment.isVisible) {
      this.searchClose()
    } else {
      super.onBackPressed()
    }
  }

  private fun tocClose() {
    SR2UIThread.checkIsUIThread()

    this.supportFragmentManager.beginTransaction()
      .hide(this.tocFragment)
      .show(this.readerFragment)
      .commit()
  }

  private fun searchClose() {
    SR2UIThread.checkIsUIThread()

    this.supportFragmentManager.beginTransaction()
      .hide(this.searchFragment)
      .show(this.readerFragment)
      .commit()
  }

  private fun onControllerBecameAvailable(reference: SR2ControllerReference) {
    this.controller = reference.controller

    // Listen for messages from the controller.
    this.controllerSubscription =
      reference.controller.events.subscribe(this::onControllerEvent)

    if (reference.isFirstStartup) {
      // Navigate to the first chapter or saved reading position.
      val database = DemoApplication.application.database()
      val bookId = reference.controller.bookMetadata.id
      reference.controller.submitCommand(SR2Command.BookmarksLoad(database.bookmarksFor(bookId)))
      val lastRead = database.bookmarkFindLastReadLocation(bookId)
      val startLocator = lastRead?.locator ?: reference.controller.bookMetadata.start
      reference.controller.submitCommand(SR2Command.OpenChapter(startLocator))
    } else {
      // Refresh whatever the controller was looking at previously.
      reference.controller.submitCommand(SR2Command.Refresh)
    }
  }

  /**
   * Start the reader with the given EPUB.
   */

  @UiThread
  private fun startReader(file: File) {
    SR2UIThread.checkIsUIThread()

    val database =
      DemoApplication.application.database()

    this.readerParameters =
      SR2ReaderParameters(
        contentProtections = emptyList(),
        bookFile = FileAsset(file),
        bookId = this.epubId!!,
        isPreview = false,
        theme = database.theme(),
        controllers = SR2Controllers(),
        scrollingMode = if (this.scrollMode.isChecked) {
          SR2ScrollingMode.SCROLLING_MODE_CONTINUOUS
        } else {
          SR2ScrollingMode.SCROLLING_MODE_PAGINATED
        },
        pageNumberingMode = if (this.perChapterPageNumbering.isChecked) {
          SR2PageNumberingMode.PER_CHAPTER
        } else {
          SR2PageNumberingMode.WHOLE_BOOK
        },
      )

    this.readerFragmentFactory =
      SR2ReaderFragmentFactory(this.readerParameters)

    this.readerFragment =
      this.readerFragmentFactory.instantiate(this.classLoader, SR2ReaderFragment::class.java.name)

    this.searchFragment =
      this.readerFragmentFactory.instantiate(this.classLoader, SR2SearchFragment::class.java.name)

    this.tocFragment =
      this.readerFragmentFactory.instantiate(this.classLoader, SR2TOCFragment::class.java.name)

    val readerModel =
      ViewModelProvider(this, SR2ReaderViewModelFactory(this.readerParameters))
        .get(SR2ReaderViewModel::class.java)

    this.viewSubscription =
      readerModel.viewEvents.subscribe(this::onViewEvent)

    val selectFileArea =
      this.findViewById<View>(R.id.selectFileArea)

    selectFileArea.visibility = View.GONE

    this.supportFragmentManager.beginTransaction()
      .add(R.id.demoFragmentArea, readerFragment)
      .commit()
  }

  /**
   * Handle incoming messages from the view fragments.
   */

  private fun onViewEvent(event: SR2ReaderViewEvent) {
    SR2UIThread.checkIsUIThread()

    return when (event) {
      SR2ReaderViewNavigationClose ->
        this.onBackPressed()
      SR2ReaderViewNavigationOpenTOC ->
        this.openTOC()
      is SR2ControllerBecameAvailable ->
        this.onControllerBecameAvailable(event.reference)
      is SR2BookLoadingFailed ->
        this.onBookLoadingFailed(event.exception)
      SR2ReaderViewNavigationOpenSearch -> {
        this.openSearch()
      }
    }
  }

  private fun onBookLoadingFailed(exception: Throwable) {
    AlertDialog.Builder(this)
      .setMessage(exception.message)
      .setOnDismissListener { this.finish() }
      .create()
      .show()
  }

  private fun openSearch() {
    val transaction = this.supportFragmentManager.beginTransaction()
      .hide(readerFragment)

    if (searchFragment.isAdded) {
      transaction.show(searchFragment)
    } else {
      transaction.add(R.id.demoFragmentArea, searchFragment)
    }

    transaction.commit()
  }

  private fun openTOC() {
    val transaction = this.supportFragmentManager.beginTransaction()
      .hide(readerFragment)

    if (tocFragment.isAdded) {
      transaction.show(tocFragment)
    } else {
      transaction.add(R.id.demoFragmentArea, tocFragment)
    }

    transaction.commit()
  }

  /**
   * Handle incoming messages from the controller.
   */

  private fun onControllerEvent(event: SR2Event) {
    when (event) {
      is SR2BookmarkCreate -> {
        val database = DemoApplication.application.database()
        database.bookmarkSave(this.controller!!.bookMetadata.id, event.bookmark)
        event.onBookmarkCreationCompleted(event.bookmark)
      }

      is SR2BookmarkDeleted -> {
        val database = DemoApplication.application.database()
        database.bookmarkDelete(this.controller!!.bookMetadata.id, event.bookmark)
      }

      is SR2ThemeChanged -> {
        val database = DemoApplication.application.database()
        database.themeSet(event.theme)
      }

      is SR2BookmarkCreated,
      is SR2OnCenterTapped,
      is SR2ReadingPositionChanged,
      SR2BookmarksLoaded,
      SR2BookmarkFailedToBeDeleted,
      is SR2BookmarkTryToDelete,
      is SR2ChapterNonexistent,
      is SR2WebViewInaccessible,
      is SR2ExternalLinkSelected,
      is SR2CommandExecutionStarted,
      is SR2CommandExecutionRunningLong,
      is SR2CommandExecutionSucceeded,
      is SR2CommandExecutionFailed,
      is SR2CommandSearchResults,
      -> {
        // Nothing
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
        val data = this.copyToStorage(uri)!!
        this.epubFile = data.first
        this.epubId = data.second
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

  private fun copyToStorage(uri: Uri): Pair<File, String>? {
    val file = File(this.filesDir, "book.epub")
    var ips: InputStream? = null
    var ops: OutputStream? = null

    try {
      ips = this.contentResolver.openInputStream(uri)
      ops = file.outputStream()
      ips?.copyTo(ops)
      return Pair(file, hashOf(file))
    } catch (e: FileNotFoundException) {
      this.logger.warn("File not found", e)
      this.showError("File not found")
    } catch (e: IOException) {
      this.logger.warn("Error copying file", e)
      this.showError("Error copying file")
    } finally {
      ips?.close()
      ops?.close()
    }

    return null
  }

  private fun hashOf(
    file: File,
  ): String {
    val digest = MessageDigest.getInstance("SHA-256")

    DigestInputStream(file.inputStream(), digest).use { input ->
      NullOutputStream().use { output ->
        input.copyTo(output)
        return digest.digest().joinToString("") { "%02x".format(it) }
      }
    }
  }

  private class NullOutputStream : OutputStream() {
    override fun write(b: Int) {
    }
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
    this.startActivityForResult(pickIntent, PICK_DOCUMENT)
  }
}
