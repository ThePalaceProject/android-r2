package org.librarysimplified.r2.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.librarysimplified.r2.views.internal.SR2TOCAdapter
import org.librarysimplified.r2.views.internal.SR2TOCBookmarksFragment
import org.librarysimplified.r2.views.internal.SR2TOCChaptersFragment
import org.librarysimplified.r2.views.internal.SR2TOCPage

class SR2TOCFragment private constructor(
  private val parameters: SR2ReaderParameters
) : Fragment() {

  companion object {
    fun create(parameters: SR2ReaderParameters): SR2TOCFragment {
      return SR2TOCFragment(parameters)
    }
  }

  private lateinit var viewPagerAdapter: SR2TOCAdapter
  private lateinit var viewPager: ViewPager2
  private lateinit var tabLayout: TabLayout
  private lateinit var toolbar: Toolbar

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view =
      inflater.inflate(R.layout.sr2_table_of_contents, container, false)

    this.tabLayout =
      view.findViewById(R.id.tocTabs)
    this.viewPager =
      view.findViewById(R.id.tocViewPager)
    this.toolbar =
      view.findViewById(R.id.tocToolbar)
    this.viewPagerAdapter =
      SR2TOCAdapter(
        fragment = this,
        pages = listOf(
          SR2TOCPage(
            title = this.resources.getString(R.string.tocTitle),
            fragmentConstructor = { SR2TOCChaptersFragment.create(this.parameters) }
          ),
          SR2TOCPage(
            title = this.resources.getString(R.string.tocBookmarks),
            fragmentConstructor = { SR2TOCBookmarksFragment.create(this.parameters) }
          )
        )
      )

    this.viewPager.adapter = this.viewPagerAdapter

    this.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
    this.toolbar.setNavigationContentDescription(R.string.settingsAccessibilityBack)

    TabLayoutMediator(this.tabLayout, this.viewPager) { tab, position ->
      tab.text = this.viewPagerAdapter.titleOf(position)
    }.attach()

    return view
  }
}
