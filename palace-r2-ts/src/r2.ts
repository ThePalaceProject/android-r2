import { requireNotNull } from './notnull';

export interface R2PageControllerType {
  recompute(documentWidth: number, pageWidth: number): void;
  pageCount(): bigint;
}

export class R2Page {
  readonly index: bigint;
  readonly scrollOffset: number;

  constructor(index: bigint, scrollOffset: number) {
    this.index = index;
    this.scrollOffset = scrollOffset;
  }
}

export class R2PageController implements R2PageControllerType {
  private pages: R2Page[];

  private constructor() {
    this.pages = [new R2Page(0n, 0.0)];
  }

  public static create(): R2PageControllerType {
    return new R2PageController();
  }

  pageCount(): bigint {
    return BigInt(this.pages.length);
  }

  recompute(documentWidth: number, pageWidth: number): void {
    requireNotNull(documentWidth, 'DocumentWidth');
    requireNotNull(pageWidth, 'PageWidth');

    const newPages: R2Page[] = [];
    const maxOffset = Math.max(0, documentWidth - pageWidth);

    let index = 0n;
    for (
      let pageOffset: number = 0;
      pageOffset < maxOffset;
      pageOffset += pageWidth
    ) {
      const newPage = new R2Page(index, pageOffset);
      ++index;
      newPages.push(newPage);
    }

    this.pages = newPages;
  }
}
