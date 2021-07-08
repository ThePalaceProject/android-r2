package org.librarysimplified.r2.api

/**
 * A navigation target. Essentially, the result of looking up a navigation node with
 * any extra fragment information that may have been provided.
 */

data class SR2NavigationTarget(
  val node: SR2NavigationNode,
  val extraFragment: String?
) {
  init {
    if (this.extraFragment != null) {
      check(this.extraFragment.isNotBlank()) {
        "Extra fragment must not be blank"
      }
      check(!this.extraFragment.contains('#')) {
        "Extra fragment '$extraFragment' must not contain #"
      }
    }
  }
}
