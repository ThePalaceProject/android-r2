/**
 * The interface exposed by the Android application. This
 * allows the reader script to call back into the application.
 */

export interface SR2AndroidType {
  /**
   * The page set is in the initial state.
   */

  onPageSetInitial(): void;

  /**
   * The page set is in the ready state.
   */

  onPageSetReady(count: number): void;

  /**
   * The page set is currently calculating pages.
   */

  onPageSetCalculating(progress: number): void;

  /**
   * Get the web view viewport width.
   */

  onGetViewportWidth(): number;

  /**
   * The reader wants the application to open the next chapter.
   */

  onWantChapterNext(): void;

  /**
   * The reader wants the application to open the previous chapter.
   */

  onWantChapterPrevious(): void;

  /**
   * The reading position has changed within the chapter.
   */

  onReadingPositionChanged(
    chapterProgress: number,
    currentPage: number,
    pageCount: number,
  ): void;

  /**
   * An error occurred.
   */

  onLogError(message: string, file: string, line: number): void;
}

declare global {
  const Android: SR2AndroidType;
}
