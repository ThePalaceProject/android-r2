import { Attribute } from './attribute';
import { requireDefined } from './notnull';

/**
 * An immutable page value. Pages are numbered starting from zero
 * and contain a scroll offset value indicating the start of the
 * page.
 */

export class SR2Page {
  readonly index: number;
  readonly scrollOffset: number;
  readonly scrollOffsetRaw: number;

  constructor(index: number, scrollOffset: number, scrollOffsetRaw: number) {
    this.index = index;
    this.scrollOffset = scrollOffset;
    this.scrollOffsetRaw = scrollOffsetRaw;

    if (this.scrollOffset < 0.0 || this.scrollOffset > 1.0) {
      throw Error(
        `Scroll offset ${this.scrollOffset.toString()} must be in the range [0, 1]`,
      );
    }
  }
}

/**
 * The interface exposed by a page set.
 */

export interface SR2PageSetType {
  pagePrevious(pageCurrent: SR2Page): SR2Page | null;
  pageNext(pageCurrent: SR2Page): SR2Page | null;
  findClosestPage(scrollOffset: number): SR2Page;
  recompute(documentWidth: number, pageWidth: number): void;
  pageCount(): number;
  pages(): SR2Page[];
  statusNow(): SR2PageSetStatus;
  readonly status: Attribute<SR2PageSetStatus>;
}

export interface SR2PageSetStatusInitial {
  kind: 'Initial';
}

export interface SR2PageSetStatusReady {
  kind: 'Ready';
}

export interface SR2PageSetStatusCalculatingPages {
  kind: 'CalculatingPages';
  progress: number;
}

export type SR2PageSetStatus =
  | SR2PageSetStatusInitial
  | SR2PageSetStatusReady
  | SR2PageSetStatusCalculatingPages;

export class SR2PageSet implements SR2PageSetType {
  private pageArray: SR2Page[];
  readonly status: Attribute<SR2PageSetStatus>;

  private constructor() {
    this.pageArray = [new SR2Page(0, 0.0, 0.0)];

    const initial: SR2PageSetStatus = {
      kind: 'Initial',
    };

    this.status = Attribute.create<SR2PageSetStatus>(initial);
  }

  public static create(): SR2PageSetType {
    return new SR2PageSet();
  }

  statusNow(): SR2PageSetStatus {
    return this.status.valueNow();
  }

  pageCount(): number {
    return this.pageArray.length;
  }

  pages(): SR2Page[] {
    return this.pageArray;
  }

  findClosestPage(scrollOffset: number): SR2Page {
    let pageNow: SR2Page = requireDefined(this.pageArray[0], 'InitialPageNow');

    for (const pageNew of this.pageArray) {
      if (pageNew.scrollOffset > scrollOffset) {
        return pageNow;
      }
      pageNow = pageNew;
    }

    requireDefined(pageNow, 'ReturnedPageNow');
    return pageNow;
  }

  recompute(documentWidth: number, pageWidth: number): void {
    requireDefined(documentWidth, 'DocumentWidth');
    requireDefined(pageWidth, 'PageWidth');

    console.log(
      `Recomputing pages: ${documentWidth.toString()} / ${pageWidth.toString()}`,
    );

    this.status.set({ kind: 'CalculatingPages', progress: 0.0 });
    const newPages: SR2Page[] = [];
    const maxOffset = Math.max(0, documentWidth - pageWidth);

    let index = 0;
    for (
      let pageOffsetRaw = 0;
      pageOffsetRaw < maxOffset;
      pageOffsetRaw += pageWidth
    ) {
      let pageOffset = 0.0;
      if (maxOffset > 0) {
        pageOffset = pageOffsetRaw / maxOffset;
      }
      const newPage = new SR2Page(index, pageOffset, pageOffsetRaw);
      ++index;
      newPages.push(newPage);
      this.status.set({
        kind: 'CalculatingPages',
        progress: pageOffset,
      });
    }

    /*
     * If the resulting pagination resulted in no pages at all, then
     * simply insert a page that represents the entire chapter.
     *
     * Otherwise, we need to check if the distance between the offset
     * of the last inserted page and the end of the document is greater
     * than one pixel. If the distance is greater than one pixel, then
     * it means that there's a displayable amount of content still left
     * over and we need to add another page to show it. Without this
     * check, the chapter has a chance to end early and miss the last
     * page.
     */

    if (newPages.length === 0) {
      newPages.push(new SR2Page(0, 0.0, 0.0));
    } else {
      const last = requireDefined(newPages[newPages.length - 1], 'LastPage');
      if (maxOffset - last.scrollOffsetRaw >= 1) {
        newPages.push(new SR2Page(index, 1.0, maxOffset));
      }
    }

    console.log(`Recomputed pages: ${newPages.length.toString()}`);
    this.pageArray = newPages;
    this.status.set({ kind: 'CalculatingPages', progress: 1.0 });
    this.status.set({ kind: 'Ready' });
  }

  pagePrevious(pageCurrent: SR2Page): SR2Page | null {
    if (pageCurrent.index === 0) {
      return null;
    }
    return requireDefined(
      this.pageArray[pageCurrent.index - 1],
      'PreviousPage',
    );
  }

  pageNext(pageCurrent: SR2Page): SR2Page | null {
    if (pageCurrent.index === this.pageArray.length - 1) {
      return null;
    }
    return requireDefined(this.pageArray[pageCurrent.index + 1], 'NextPage');
  }
}
