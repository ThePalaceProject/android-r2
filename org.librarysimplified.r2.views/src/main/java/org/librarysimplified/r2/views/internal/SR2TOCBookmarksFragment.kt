package org.librarysimplified.r2.views.internal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Bookmark.Type.LAST_READ
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.SR2ReaderModel
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationTOCClose
import org.slf4j.LoggerFactory

internal class SR2TOCBookmarksFragment : Fragment() {

  private val logger =
    LoggerFactory.getLogger(SR2TOCBookmarksFragment::class.java)

  private lateinit var lastReadItem: SR2TOCBookmarkViewHolder
  private lateinit var emptyMessage: TextView
  private lateinit var bookmarkAdapter: SR2TOCBookmarkAdapter
  private lateinit var bookmarkSubscriptions: CompositeDisposable

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

    this.emptyMessage = layout.findViewById(R.id.empty_bookmarks_text)

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

    this.bookmarkSubscriptions = CompositeDisposable()
    this.bookmarkSubscriptions.add(
      SR2ReaderModel.controllerEvents
        .ofType(SR2BookmarkEvent::class.java)
        .subscribe { this.reloadBookmarks() },
    )

    this.reloadBookmarks()
  }

  override fun onStop() {
    super.onStop()
    this.bookmarkSubscriptions.dispose()
  }

  private fun reloadBookmarks() {
    SR2UIThread.runOnUIThread {
      this.reloadBookmarksUI()
    }
  }

  @UiThread
  private fun reloadBookmarksUI() {
    SR2UIThread.checkIsUIThread()

    val bookmarksNow = SR2ReaderModel.controller().bookmarksNow()
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

    this.emptyMessage.visibility = if (bookmarks.isEmpty() && lastRead == null) {
      View.VISIBLE
    } else {
      View.GONE
    }
  }

  private fun onBookmarkSelected(bookmark: SR2Bookmark) {
    SR2ReaderModel.controller().submitCommand(SR2Command.OpenChapter(bookmark.locator))

    SR2UIThread.runOnUIThreadDelayed(
      { SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationTOCClose) },
      SR2TOC.tocSelectionDelay(),
    )
  }

  private fun onBookmarkDeleteRequested(bookmark: SR2Bookmark) {
    SR2ReaderModel.controller().submitCommand(SR2Command.BookmarkDelete(bookmark))
  }
}
