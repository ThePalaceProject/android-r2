import { Attribute } from './attribute';
import { requireNotNull } from './notnull';

/**
 * The interface exposed by a page controller.
 */

export interface R2PageControllerType {
  recompute(documentWidth: number, pageWidth: number): void;
  pageCount(): bigint;
  statusNow(): R2PageControllerStatus;
  readonly status: Attribute<R2PageControllerStatus>;
}

export interface R2PageControllerStatusReady {
  kind: 'Ready';
}

export interface R2PageControllerStatusCalculatingPages {
  kind: 'CalculatingPages';
  progress: number;
}

export type R2PageControllerStatus =
  R2PageControllerStatusReady | R2PageControllerStatusCalculatingPages;

/**
 * An immutable page value. Pages are numbered starting from zero
 * and contain a scroll offset value indicating the start of the
 * page.
 */

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
  readonly status: Attribute<R2PageControllerStatus>;

  private constructor() {
    this.pages = [new R2Page(0n, 0.0)];

    const initial: R2PageControllerStatus = {
      kind: 'Ready',
    };

    this.status = Attribute.create<R2PageControllerStatus>(initial);
  }

  public static create(): R2PageControllerType {
    return new R2PageController();
  }

  statusNow(): R2PageControllerStatus {
    return this.status.valueNow();
  }

  pageCount(): bigint {
    return BigInt(this.pages.length);
  }

  recompute(documentWidth: number, pageWidth: number): void {
    requireNotNull(documentWidth, 'DocumentWidth');
    requireNotNull(pageWidth, 'PageWidth');

    this.status.set({ kind: 'CalculatingPages', progress: 0.0 });

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
      const progress = pageOffset / maxOffset;
      newPages.push(newPage);
      this.status.set({
        kind: 'CalculatingPages',
        progress: progress,
      });
    }

    this.pages = newPages;
    this.status.set({ kind: 'CalculatingPages', progress: 1.0 });
    this.status.set({ kind: 'Ready' });
  }
}
