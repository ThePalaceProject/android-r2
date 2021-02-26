package org.librarysimplified.r2.views

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.librarysimplified.r2.views.internal.SR2TOCBookmarksFragment
import org.librarysimplified.r2.views.internal.SR2TOCChaptersFragment

class SR2ReaderFragmentFactory(
  private val parameters: SR2ReaderParameters
) : FragmentFactory() {

  override fun instantiate(
    classLoader: ClassLoader,
    className: String
  ): Fragment {
    val clazz: Class<*> = loadFragmentClass(classLoader, className)

    return when (clazz) {
      SR2ReaderFragment::class.java ->
        SR2ReaderFragment.create(this.parameters)
      SR2TOCBookmarksFragment::class.java ->
        SR2TOCBookmarksFragment.create(this.parameters)
      SR2TOCChaptersFragment::class.java ->
        SR2TOCChaptersFragment.create(this.parameters)
      else ->
        super.instantiate(classLoader, className)
    }
  }
}
