import { Attribute } from './attribute';
import { requireNotNull } from './notnull';

/**
 * An immutable page value. Pages are numbered starting from zero
 * and contain a scroll offset value indicating the start of the
 * page.
 */

export class SR2Page {
  readonly index: bigint;
  readonly scrollOffset: number;
  readonly scrollOffsetRaw: number;

  constructor(index: bigint, scrollOffset: number, scrollOffsetRaw: number) {
    this.index = index;
    this.scrollOffset = scrollOffset;
    this.scrollOffsetRaw = scrollOffsetRaw;

    if (this.scrollOffset < 0.0 || this.scrollOffset > 1.0) {
      throw Error(
        'Scroll offset ${this.scrollOffset} must be in the range [0, 1]',
      );
    }
  }
}

/**
 * The interface exposed by a page set.
 */

export interface SR2PageSetType {
  findClosestPage(scrollOffset: number): SR2Page;
  recompute(documentWidth: number, pageWidth: number): void;
  pageCount(): bigint;
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
    this.pageArray = [new SR2Page(0n, 0.0, 0.0)];

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

  pageCount(): bigint {
    return BigInt(this.pageArray.length);
  }

  pages(): SR2Page[] {
    return this.pageArray;
  }

  findClosestPage(scrollOffset: number): SR2Page {
    let pageNow = this.pageArray[0]!;
    for (const pageNew of this.pageArray) {
      if (pageNew.scrollOffset > scrollOffset) {
        return pageNow;
      }
      pageNow = pageNew;
    }
    return pageNow;
  }

  recompute(documentWidth: number, pageWidth: number): void {
    requireNotNull(documentWidth, 'DocumentWidth');
    requireNotNull(pageWidth, 'PageWidth');

    this.status.set({ kind: 'CalculatingPages', progress: 0.0 });

    const newPages: SR2Page[] = [];
    const maxOffset = Math.max(0, documentWidth - pageWidth);

    let index = 0n;
    for (
      let pageOffsetRaw: number = 0;
      pageOffsetRaw < maxOffset;
      pageOffsetRaw += pageWidth
    ) {
      const pageOffset = pageOffsetRaw / maxOffset;
      const newPage = new SR2Page(index, pageOffset, pageOffsetRaw);
      ++index;
      newPages.push(newPage);
      this.status.set({
        kind: 'CalculatingPages',
        progress: pageOffset,
      });
    }

    this.pageArray = newPages;
    this.status.set({ kind: 'CalculatingPages', progress: 1.0 });
    this.status.set({ kind: 'Ready' });
  }
}
