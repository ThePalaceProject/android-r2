package org.librarysimplified.r2.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationTOCClose
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookLoadingFailed
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameAvailable
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameUnavailable
import org.librarysimplified.r2.views.internal.SR2TOCBookmarksFragment
import org.librarysimplified.r2.views.internal.SR2TOCChaptersFragment
import org.librarysimplified.r2.views.internal.SR2TOCPagesFragment
import org.librarysimplified.r2.views.internal.SR2TOCPage

class SR2TOCFragment : SR2Fragment() {
  private lateinit var eventSubscriptions: CompositeDisposable

  private lateinit var tocLayout: View
  private lateinit var tocToolbarBack: View
  private lateinit var tocTabs: TabLayout
  private lateinit var tocViewPager: ViewPager2
  private lateinit var viewPagerAdapter: SR2TOCAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    this.tocLayout =
      inflater.inflate(R.layout.sr2_table_of_contents, container, false)
    this.tocToolbarBack =
      this.tocLayout.findViewById(R.id.tocToolbarBack)
    this.tocTabs =
      this.tocLayout.findViewById(R.id.tocTabs)
    this.tocViewPager =
      this.tocLayout.findViewById(R.id.tocViewPager)

    // Set up back button
    this.tocToolbarBack.setOnClickListener {
      this.submitCommand(SR2ReaderViewNavigationTOCClose)
    }

    // Set up ViewPager2 with initial empty adapter
    this.viewPagerAdapter =
      SR2TOCAdapter(this)
    this.tocViewPager.adapter = this.viewPagerAdapter

    return this.tocLayout
  }

  override fun onStart() {
    super.onStart()
    this.eventSubscriptions = CompositeDisposable()
    this.eventSubscriptions.add(SR2ReaderModel.viewEvents.subscribe(this::onViewEvent))
  }

  override fun onStop() {
    super.onStop()
    this.eventSubscriptions.dispose()
  }

  private fun onViewEvent(event: SR2ReaderViewEvent) {
    when (event) {
      is SR2BookLoadingFailed -> {
        // Nothing to do here.
      }

      is SR2ControllerBecameAvailable -> {
        this.onControllerAvailable()
      }

      is SR2ControllerBecameUnavailable -> {
        this.onControllerUnavailable()
      }

      else -> {
        // Ignored.
      }
    }
  }

  private fun onControllerAvailable() {
    val controller = SR2ReaderModel.controllerNow()
    if (controller == null) {
      return
    }
    val hasPrintPages = controller.bookMetadata.printPages.isNotEmpty()
    this.setupTabs(hasPrintPages)
  }

  private fun onControllerUnavailable() {
    // Nothing to do here.
  }

  private fun setupTabs(hasPrintPages: Boolean) {
    val tabs =
      if (hasPrintPages) {
        listOf(
          SR2TOCPage(this.getString(R.string.tocTitle)) {
            SR2TOCChaptersFragment()
          },
          SR2TOCPage(this.getString(R.string.tocPages)) {
            SR2TOCPagesFragment()
          },
          SR2TOCPage(this.getString(R.string.tocBookmarks)) {
            SR2TOCBookmarksFragment()
          },
        )
      } else {
        listOf(
          SR2TOCPage(this.getString(R.string.tocTitle)) {
            SR2TOCChaptersFragment()
          },
          SR2TOCPage(this.getString(R.string.tocBookmarks)) {
            SR2TOCBookmarksFragment()
          },
        )
      }

    this.viewPagerAdapter.setPages(tabs)
    this.tocTabs.removeAllTabs()

    // Use TabLayoutMediator to sync tabs with ViewPager2 pages
    TabLayoutMediator(this.tocTabs, this.tocViewPager) { tab, position ->
      tab.text = tabs[position].title
    }.attach()
  }

  private fun submitCommand(command: SR2ReaderViewCommand) {
    SR2ReaderModel.submitViewCommand(command)
  }

  internal inner class SR2TOCAdapter(
    fragment: Fragment,
  ) : FragmentStateAdapter(fragment) {
    private var pages: List<SR2TOCPage> = emptyList()

    fun setPages(pages: List<SR2TOCPage>) {
      this.pages = pages
      this.notifyDataSetChanged()
    }

    override fun getItemCount(): Int = this.pages.size

    override fun createFragment(position: Int): Fragment = this.pages[position].fragmentConstructor()
  }
}
