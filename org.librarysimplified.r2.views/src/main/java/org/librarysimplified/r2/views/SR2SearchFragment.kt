package org.librarysimplified.r2.views

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionFailed
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionSucceeded
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionRunningLong
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionStarted
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandSearchResults
import org.librarysimplified.r2.api.SR2Event.SR2Error
import org.librarysimplified.r2.api.SR2Event.SR2ExternalLinkSelected
import org.librarysimplified.r2.api.SR2Event.SR2OnCenterTapped
import org.librarysimplified.r2.api.SR2Event.SR2ReadingPositionChanged
import org.librarysimplified.r2.api.SR2Event.SR2ThemeChanged
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationSearchClose
import org.librarysimplified.r2.views.internal.SR2Ripples
import org.librarysimplified.r2.views.search.SR2SearchResultAdapter
import org.librarysimplified.r2.views.search.SR2SearchResultSectionItemDecoration
import org.librarysimplified.r2.views.search.SR2SearchResultSectionListener
import org.readium.r2.shared.publication.Href
import org.slf4j.LoggerFactory

class SR2SearchFragment : SR2Fragment() {

  private val logger =
    LoggerFactory.getLogger(SR2SearchFragment::class.java)

  private lateinit var buttonBack: View
  private lateinit var buttonBackIcon: ImageView
  private lateinit var noResultLabel: TextView
  private lateinit var searchAdapter: SR2SearchResultAdapter
  private lateinit var searchEditText: EditText
  private lateinit var searchResultsList: RecyclerView
  private lateinit var subscriptions: CompositeDisposable
  private lateinit var toolbar: ViewGroup
  private lateinit var toolbarButtons: List<View>

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R.layout.sr2_search, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)

    this.toolbar =
      view.findViewById(R.id.searchToolbar2)
    this.buttonBack =
      this.toolbar.findViewById(R.id.searchToolbarBackTouch)
    this.buttonBackIcon =
      this.toolbar.findViewById(R.id.searchToolbarBack)

    this.searchResultsList =
      view.findViewById(R.id.searchResultsList)
    this.noResultLabel =
      view.findViewById(R.id.noResultLabel)
    this.searchEditText =
      view.findViewById(R.id.searchToolbarSearchText)

    this.buttonBack.setOnClickListener {
      SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationSearchClose)
    }

    this.toolbarButtons = listOf(this.buttonBack)
    this.toolbarButtons.forEach { v ->
      v.foreground = SR2Ripples.createRippleDrawableForLightBackground()
    }

    this.searchAdapter = SR2SearchResultAdapter(
      onItemClicked = { locator ->
        SR2ReaderModel.submitCommand(
          SR2Command.HighlightTerms(
            searchingTerms = this.searchEditText.text.toString(),
            clearHighlight = false,
          ),
        )

        SR2ReaderModel.submitCommand(
          SR2Command.OpenChapter(
            SR2Locator.SR2LocatorPercent.create(
              chapterHref = Href(locator.href),
              chapterProgress = locator.locations.progression ?: 0.0,
            ),
          ),
        )
        this.close()
      },
    )

    this.searchResultsList.apply {
      this.adapter = this@SR2SearchFragment.searchAdapter
      this.layoutManager = LinearLayoutManager(
        this@SR2SearchFragment.requireContext(),
        LinearLayoutManager.VERTICAL,
        false,
      )
      this.addItemDecoration(
        SR2SearchResultSectionItemDecoration(
          this.context,
          object : SR2SearchResultSectionListener {
            override fun isStartOfSection(index: Int): Boolean =
              SR2ReaderModel.searchLocators.value.run {
                when {
                  index == 0 -> {
                    true
                  }

                  index < 0 || index >= size -> {
                    false
                  }

                  else -> {
                    getOrNull(index)?.title != getOrNull(index - 1)?.title
                  }
                }
              }

            override fun sectionTitle(index: Int): String {
              return SR2ReaderModel.searchLocators.value.getOrNull(index)?.title.orEmpty()
            }
          },
        ),
      )
      this.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
    }

    this.configureSearch()
  }

  @UiThread
  private fun close() {
    SR2UIThread.checkIsUIThread()
    SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationSearchClose)
  }

  private fun configureSearch() {
    this.searchEditText.setOnEditorActionListener { v, actionId, event ->
      val searchText = v.text.trim().toString()
      SR2ReaderModel.submitCommand(SR2Command.Search(searchText))
      true
    }

    SR2ReaderModel.searchResult
      .onEach { this.searchAdapter.submitData(it) }
      .launchIn(this.viewLifecycleOwner.lifecycleScope)

    SR2ReaderModel.searchLocators
      .onEach { this.noResultLabel.isVisible = it.isEmpty() }
      .launchIn(this.viewLifecycleOwner.lifecycleScope)
  }

  override fun onStart() {
    super.onStart()

    this.subscriptions = CompositeDisposable()
    this.subscriptions.add(SR2ReaderModel.controllerEvents.subscribe(this::onControllerEvent))

    this.searchEditText.postDelayed({
      this.searchEditText.requestFocus()
    }, 250L)

    this.searchEditText.postDelayed({
      try {
        val activity = this.requireActivity()
        val input = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        input.showSoftInput(this.searchEditText.findFocus(), InputMethodManager.SHOW_FORCED)
      } catch (e: Throwable) {
        this.logger.debug("Failed to show keyboard: ", e)
      }
    }, 250L)
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.dispose()
  }

  @UiThread
  private fun onControllerEvent(event: SR2Event) {
    SR2UIThread.checkIsUIThread()

    when (event) {
      is SR2ReadingPositionChanged,
      is SR2BookmarkDeleted,
      is SR2BookmarkCreated,
      is SR2ThemeChanged,
      is SR2Error.SR2ChapterNonexistent,
      is SR2Error.SR2WebViewInaccessible,
      is SR2OnCenterTapped,
      is SR2CommandExecutionRunningLong,
      is SR2CommandExecutionStarted,
      is SR2CommandExecutionSucceeded,
      is SR2CommandExecutionFailed,
      is SR2ExternalLinkSelected,
      -> {
        // Nothing
      }

      is SR2CommandSearchResults -> {
        SR2ReaderModel.consumeSearchResults(event)
      }
    }
  }
}
