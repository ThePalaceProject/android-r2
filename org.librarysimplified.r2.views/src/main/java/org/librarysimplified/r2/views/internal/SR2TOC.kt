package org.librarysimplified.r2.views.internal

internal object SR2TOC {

  /**
   * The delay applied after selecting a TOC item. This purely exists to allow the
   * selection animation time to complete before the TOC fragment is closed.
   */

  fun tocSelectionDelay(): Long {
    return 300L
  }
}
