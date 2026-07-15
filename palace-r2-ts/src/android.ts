export interface SR2AndroidType {
  onPageSetInitial(): void;

  onPageSetReady(count: number): void;

  onPageSetCalculating(progress: number): void;

  onGetViewportWidth(): number;

  onWantChapterNext(): void;

  onWantChapterPrevious(): void;

  onReadingPositionChanged(
    chapterProgress: number,
    currentPage: number,
    pageCount: number,
  ): void;

  onLogError(message: string, file: string, line: number): void;
}

declare global {
  const Android: SR2AndroidType;
}
