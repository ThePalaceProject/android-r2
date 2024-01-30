package org.librarysimplified.r2.demo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.runBlocking
import org.librarysimplified.r2.api.SR2Command
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
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.vanilla.SR2Controllers
import org.librarysimplified.r2.views.SR2ControllerReference
import org.librarysimplified.r2.views.SR2ReaderFragment
import org.librarysimplified.r2.views.SR2ReaderModel
import org.librarysimplified.r2.views.SR2ReaderViewCommand
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationReaderClose
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationSearchClose
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationSearchOpen
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationTOCClose
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationTOCOpen
import org.librarysimplified.r2.views.SR2ReaderViewEvent
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookLoadingFailed
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameAvailable
import org.librarysimplified.r2.views.SR2TOCFragment
import org.librarysimplified.r2.views.search.SR2SearchFragment
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest

class DemoActivity : AppCompatActivity(R.layout.demo_activity_host) {

  private val logger =
    LoggerFactory.getLogger(DemoActivity::class.java)

  companion object {
    const val PICK_DOCUMENT = 1001
  }

  private lateinit var subscriptions: CompositeDisposable
  private var fragmentNow: Fragment? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val toolbar = this.findViewById(R.id.mainToolbar) as Toolbar
    this.setSupportActionBar(toolbar)

    this.subscriptions = CompositeDisposable()
    this.fragmentNow = null

    this.switchFragment(DemoFileSelectionFragment())
  }

  @Deprecated("Deprecated in Java")
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

    this.subscriptions.add(
      SR2ReaderModel.controllerEvents.subscribe(this::onControllerEvent),
    )
    this.subscriptions.add(
      SR2ReaderModel.viewCommands.subscribe(this::onViewCommandReceived),
    )
    this.subscriptions.add(
      SR2ReaderModel.viewEvents.subscribe(this::onViewEventReceived),
    )
  }

  override fun onStop() {
    super.onStop()

    val fragment = this.fragmentNow
    if (fragment != null) {
      this.supportFragmentManager.beginTransaction()
        .remove(fragment)
        .commitAllowingStateLoss()
    }

    this.subscriptions.dispose()
  }

  @UiThread
  private fun onControllerBecameAvailable(reference: SR2ControllerReference) {
    this.switchFragment(SR2ReaderFragment())

    if (reference.isFirstStartup) {
      // Navigate to the first chapter or saved reading position.
      val bookId = reference.controller.bookMetadata.id
      reference.controller.submitCommand(
        SR2Command.BookmarksLoad(DemoModel.database.bookmarksFor(bookId)),
      )
      val lastRead = DemoModel.database.bookmarkFindLastReadLocation(bookId)
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
  private fun startReader() {
    SR2UIThread.checkIsUIThread()

    this.logger.debug("Opening asset...")
    val assetRetriever =
      AssetRetriever(
        contentResolver = this.contentResolver,
        httpClient = DefaultHttpClient(),
      )

    val file = DemoModel.epubFile!!
    val id = DemoModel.epubId!!

    val asset =
      when (val a = runBlocking { assetRetriever.retrieve(file) }) {
        is Try.Failure -> TODO()
        is Try.Success -> a.value
      }

    SR2ReaderModel.controllerCreate(
      contentProtections = emptyList(),
      bookFile = asset,
      bookId = id,
      theme = DemoModel.database.theme(),
      context = DemoApplication.application,
      controllers = SR2Controllers(),
    )
  }

  /**
   * Handle incoming messages from the view fragments.
   */

  @UiThread
  private fun onViewEventReceived(event: SR2ReaderViewEvent) {
    SR2UIThread.checkIsUIThread()

    return when (event) {
      is SR2ControllerBecameAvailable ->
        this.onControllerBecameAvailable(event.reference)

      is SR2BookLoadingFailed ->
        this.onBookLoadingFailed(event.exception)
    }
  }

  @UiThread
  private fun onViewCommandReceived(command: SR2ReaderViewCommand) {
    SR2UIThread.checkIsUIThread()

    return when (command) {
      SR2ReaderViewNavigationSearchOpen -> {
        this.switchFragment(SR2SearchFragment())
      }
      SR2ReaderViewNavigationTOCOpen -> {
        this.switchFragment(SR2TOCFragment())
      }
      SR2ReaderViewNavigationReaderClose -> {
        this.switchFragment(DemoFileSelectionFragment())
      }
      SR2ReaderViewNavigationSearchClose -> {
        this.switchFragment(SR2ReaderFragment())
      }
      SR2ReaderViewNavigationTOCClose -> {
        this.switchFragment(SR2ReaderFragment())
      }
    }
  }

  private fun switchFragment(fragment: Fragment) {
    this.fragmentNow = fragment
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, fragment)
      .commit()
  }

  private fun onBookLoadingFailed(exception: Throwable) {
    AlertDialog.Builder(this)
      .setMessage(exception.message)
      .setOnDismissListener { this.finish() }
      .create()
      .show()
  }

  /**
   * Handle incoming messages from the controller.
   */

  private fun onControllerEvent(event: SR2Event) {
    when (event) {
      is SR2BookmarkCreate -> {
        DemoModel.database.bookmarkSave(
          SR2ReaderModel.controller().bookMetadata.id,
          event.bookmark,
        )
        event.onBookmarkCreationCompleted(event.bookmark)
      }

      is SR2BookmarkDeleted -> {
        DemoModel.database.bookmarkDelete(
          SR2ReaderModel.controller().bookMetadata.id,
          event.bookmark,
        )
      }

      is SR2ThemeChanged -> {
        DemoModel.database.themeSet(event.theme)
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
    SR2UIThread.checkIsUIThread()
    this.switchFragment(DemoLoadingFragment())

    // Assume the user picked a valid EPUB file. In reality, we'd want to verify
    // this is a supported file type.
    if (resultCode == Activity.RESULT_OK) {
      intent?.data?.let { uri ->
        // This copy operation should be done on a worker thread; omitted for brevity.
        val data = this.copyToStorage(uri)!!
        DemoModel.setEpubAndId(data)

        this.logger.debug("Starting reader...")
        this.startReader()
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
      return Pair(file, this.hashOf(file))
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
}
