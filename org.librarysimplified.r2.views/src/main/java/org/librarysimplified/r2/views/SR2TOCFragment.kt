package org.librarysimplified.r2.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationSearchClose
import org.librarysimplified.r2.views.internal.SR2Ripples
import org.librarysimplified.r2.views.internal.SR2TOCAdapter
import org.librarysimplified.r2.views.internal.SR2TOCBookmarksFragment
import org.librarysimplified.r2.views.internal.SR2TOCChaptersFragment
import org.librarysimplified.r2.views.internal.SR2TOCPage

class SR2TOCFragment : SR2Fragment() {

  private lateinit var buttonBack: View
  private lateinit var buttonBackIcon: ImageView
  private lateinit var tabLayout: TabLayout
  private lateinit var toolbar: ViewGroup
  private lateinit var toolbarButtons: List<View>
  private lateinit var viewPager: ViewPager2
  private lateinit var viewPagerAdapter: SR2TOCAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val view =
      inflater.inflate(R.layout.sr2_table_of_contents, container, false)

    this.toolbar =
      view.findViewById(R.id.tocToolbar2)
    this.buttonBack =
      this.toolbar.findViewById(R.id.tocToolbarBackTouch)
    this.buttonBackIcon =
      this.toolbar.findViewById(R.id.tocToolbarBack)

    this.tabLayout =
      view.findViewById(R.id.tocTabs)
    this.viewPager =
      view.findViewById(R.id.tocViewPager)
    this.viewPagerAdapter =
      SR2TOCAdapter(
        fragment = this,
        pages = listOf(
          SR2TOCPage(
            title = this.resources.getString(R.string.tocTitle),
            fragmentConstructor = { SR2TOCChaptersFragment() },
          ),
          SR2TOCPage(
            title = this.resources.getString(R.string.tocBookmarks),
            fragmentConstructor = { SR2TOCBookmarksFragment() },
          ),
        ),
      )

    this.buttonBack.setOnClickListener {
      SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationSearchClose)
    }

    this.toolbarButtons = listOf(this.buttonBack)
    this.toolbarButtons.forEach { v ->
      v.foreground = SR2Ripples.createRippleDrawableForLightBackground()
    }

    this.viewPager.adapter = this.viewPagerAdapter

    TabLayoutMediator(this.tabLayout, this.viewPager) { tab, position ->
      tab.text = this.viewPagerAdapter.titleOf(position)
    }.attach()

    return view
  }
}
