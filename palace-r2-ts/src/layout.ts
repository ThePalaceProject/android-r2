/**
 * The EPUB publication layout, as declared in the publication metadata.
 */

export enum SR2EPUBLayout {
  /**
   * The publication has fixed-size pages that must not be reflowed.
   */

  SR2_FIXED = 'SR2_FIXED',

  /**
   * The publication has reflowable content that can adapt to the viewport.
   */

  SR2_REFLOWABLE = 'SR2_REFLOWABLE',
}
