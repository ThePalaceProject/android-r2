package org.librarysimplified.r2.views.internal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Bookmark.Type.LAST_READ
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.SR2ReaderParameters
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationClose
import org.librarysimplified.r2.views.SR2ReaderViewModel
import org.librarysimplified.r2.views.SR2ReaderViewModelFactory
import org.slf4j.LoggerFactory

internal class SR2TOCBookmarksFragment private constructor(
  private val parameters: SR2ReaderParameters,
) : Fragment() {

  private val logger = LoggerFactory.getLogger(SR2TOCBookmarksFragment::class.java)

  companion object {
    fun create(parameters: SR2ReaderParameters): SR2TOCBookmarksFragment {
      return SR2TOCBookmarksFragment(parameters)
    }
  }

  private lateinit var lastReadItem: SR2TOCBookmarkViewHolder
  private lateinit var emptyMessage: TextView
  private lateinit var bookmarkAdapter: SR2TOCBookmarkAdapter
  private lateinit var controller: SR2ControllerType
  private lateinit var readerModel: SR2ReaderViewModel
  private val bookmarkSubscriptions = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.bookmarkAdapter =
      SR2TOCBookmarkAdapter(
        resources = this.resources,
        onBookmarkSelected = {
          this.onBookmarkSelected(it)
        },
        onBookmarkDeleteRequested = {
          this.onBookmarkDeleteRequested(it)
        },
      )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val layout =
      inflater.inflate(R.layout.sr2_toc_bookmarks, container, false)

    val recyclerView =
      layout.findViewById<RecyclerView>(R.id.tocBookmarksList)

    emptyMessage = layout.findViewById(R.id.empty_bookmarks_text)

    recyclerView.adapter = this.bookmarkAdapter
    recyclerView.setHasFixedSize(true)
    recyclerView.setItemViewCacheSize(32)
    recyclerView.layoutManager = LinearLayoutManager(this.context)
    (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.lastReadItem =
      SR2TOCBookmarkViewHolder(layout.findViewById<ViewGroup>(R.id.tocBookmarksLastRead))
    return layout
  }

  override fun onStart() {
    super.onStart()

    val activity = this.requireActivity()

    this.readerModel =
      ViewModelProvider(activity, SR2ReaderViewModelFactory(this.parameters))
        .get(SR2ReaderViewModel::class.java)

    this.controller =
      this.readerModel.createOrGet(
        configuration = SR2ControllerConfiguration(
          bookFile = this.parameters.bookFile,
          bookId = this.parameters.bookId,
          context = activity,
          ioExecutor = this.readerModel.ioExecutor,
          contentProtections = this.parameters.contentProtections,
          theme = this.parameters.theme,
          uiExecutor = SR2UIThread::runOnUIThread,
          scrollingMode = this.parameters.scrollingMode,
          pageNumberingMode = this.parameters.pageNumberingMode,
        ),
      ).get().controller

    this.bookmarkSubscriptions.add(
      this.controller.events.ofType(SR2BookmarkEvent::class.java)
        .subscribe { this.reloadBookmarks() },
    )

    this.reloadBookmarks()
  }

  private fun reloadBookmarks() {
    SR2UIThread.runOnUIThread {
      this.reloadBookmarksUI()
    }
  }

  @UiThread
  private fun reloadBookmarksUI() {
    SR2UIThread.checkIsUIThread()

    val bookmarksNow = this.controller.bookmarksNow()
    this.logger.debug("received {} bookmarks", bookmarksNow.size)
    val bookmarks = bookmarksNow.filter { it.type != LAST_READ }
    this.bookmarkAdapter.setBookmarks(bookmarks)

    val lastRead = bookmarksNow.find { it.type == LAST_READ }
    if (lastRead != null) {
      this.lastReadItem.rootView.visibility = View.VISIBLE
      this.lastReadItem.bindTo(
        resources = this.resources,
        onBookmarkSelected = { },
        onBookmarkDeleteRequested = { },
        bookmark = lastRead,
      )
    } else {
      this.lastReadItem.rootView.visibility = View.GONE
    }

    emptyMessage.visibility = if (bookmarks.isEmpty() && lastRead == null) {
      View.VISIBLE
    } else {
      View.GONE
    }
  }

  private fun onBookmarkSelected(bookmark: SR2Bookmark) {
    this.controller.submitCommand(SR2Command.OpenChapter(bookmark.locator))

    SR2UIThread.runOnUIThreadDelayed(
      Runnable { this.readerModel.publishViewEvent(SR2ReaderViewNavigationClose) },
      SR2TOC.tocSelectionDelay(),
    )
  }

  private fun onBookmarkDeleteRequested(bookmark: SR2Bookmark) {
    this.controller.submitCommand(SR2Command.BookmarkDelete(bookmark))
  }

  override fun onStop() {
    super.onStop()
    this.bookmarkSubscriptions.dispose()
  }
}
