import { SR2APIType } from './api';
import { SR2Gestures } from './gestures';
import { requireNotNull } from './notnull';
import { SR2Page, SR2PageSet } from './pageset';
import { putSettings, SR2SettingsType } from './settings';
import { unreachable } from './unreachable';

const pageSet = SR2PageSet.create();
let pageCurrent: SR2Page = pageSet.pages()[0]!;

/** Set the current page. */

function setPage(page: SR2Page) {
  requireNotNull(page, 'Page');
  console.log(`Setting current page to: ${JSON.stringify(page)}`);
  pageCurrent = page;
}

/*
 * Tell the native application about the status of the page set. This allows
 * the application to show a progress bar whilst the pages are being calculated.
 */

pageSet.status.subscribe((_, statusNew) => {
  const kind = statusNew.kind;
  switch (kind) {
    case 'Initial': {
      Android.onPageSetInitial();
      break;
    }
    case 'Ready': {
      Android.onPageSetReady(pageSet.pageCount());
      break;
    }
    case 'CalculatingPages': {
      Android.onPageSetCalculating(statusNew.progress);
      break;
    }
    default: {
      unreachable(kind);
    }
  }
});

function isRTL() {
  return document.body.dir.toLowerCase() == 'rtl';
}

/**
 * The main function that scrolls to a page. This must be the one and only
 * function that writes to the document's scroll element, for the sake of
 * maintainability.
 */

function onScrollToPosition(page: SR2Page) {
  requireNotNull(page, 'Page');

  const scrollElement = document.scrollingElement;
  if (scrollElement == null) {
    console.warn('Document scroll element is null');
    return;
  }

  const factor = isRTL() ? -1 : 1;
  scrollElement.scrollLeft = page.scrollOffsetRaw * factor;
  setPage(page);

  Android.onReadingPositionChanged(
    page.scrollOffset,
    page.index + 1,
    pageSet.pageCount(),
  );
}

function onWantPagePrevious() {
  const page: SR2Page | null = pageSet.pagePrevious(pageCurrent);
  if (page == null) {
    Android.onWantChapterPrevious();
  } else {
    onScrollToPosition(page);
  }
}

function onWantPageNext() {
  const page: SR2Page | null = pageSet.pageNext(pageCurrent);
  if (page == null) {
    Android.onWantChapterNext();
  } else {
    onScrollToPosition(page);
  }
}

const gestures = SR2Gestures.create({
  window: window,
  onSwipeLeft: () => onWantPageNext(),
  onSwipeRight: () => onWantPagePrevious(),
  onTapLeft: () => onWantPagePrevious(),
  onTapRight: () => onWantPageNext(),
});

let onViewportWidthChangedExecuting = false;

function onViewportWidthChanged(): void {
  if (onViewportWidthChangedExecuting) {
    return;
  }

  try {
    onViewportWidthChangedExecuting = true;

    console.log('onViewportWidthChanged');

    if (document == null) {
      throw Error('Document is null!');
    }
    const scrollingElement = document.scrollingElement;
    if (scrollingElement == null) {
      throw Error('Document scrolling element is null!');
    }

    const documentWidth = scrollingElement.scrollWidth;
    // We can't rely on window.innerWidth for the pageWidth on Android, because if the
    // device pixel ratio is not an integer, we get rounding issues offsetting the pages.
    //
    // See https://github.com/readium/readium-css/issues/97
    // and https://github.com/readium/r2-navigator-kotlin/issues/146
    const width = Android.onGetViewportWidth();
    const pageWidth = width / window.devicePixelRatio;

    const root = document.documentElement;
    root.style.setProperty(
      '--RS__viewportWidth',
      'calc(' + width + 'px / ' + window.devicePixelRatio + ')',
    );

    pageSet.recompute(documentWidth, pageWidth);
  } finally {
    onViewportWidthChangedExecuting = false;
  }
}

function onPutSettings(settings: SR2SettingsType) {
  putSettings(settings);
  onViewportWidthChanged();
}

/**
 * The API exposed to the native application.
 */

export const api: SR2APIType = {
  highlightSearchingTerms: function (
    _searchingTerms: string,
    _clearHighlight: boolean,
  ): void {
    throw new Error('Function not implemented.');
  },
  turnPageLeft: function () {
    onWantPagePrevious();
  },
  turnPageRight: function () {
    onWantPageNext();
  },
  goToPosition: function (offset: number): void {
    onScrollToPosition(pageSet.findClosestPage(offset));
  },
  goToId: function (_id: string): void {
    throw new Error('Function not implemented.');
  },
  putSettings: function (settings: SR2SettingsType): void {
    onPutSettings(settings);
  },
};

/*
 * Expose the API in a manner that will survive minification.
 */

declare global {
  interface Window {
    api: SR2APIType;
  }
}

window.api = api;

/*
 * Register an event listener so that the Android app can receive the error messages.
 */

window.addEventListener(
  'error',
  function (event) {
    Android.onLogError(event.message, event.filename, event.lineno);
  },
  false,
);

/*
 * Respond to orientation changes and other events once the page has loaded.
 */

window.addEventListener(
  'load',
  function () {
    window.addEventListener('orientationchange', function () {
      onViewportWidthChanged();
    });

    window.document.addEventListener('touchstart', (event) => {
      gestures.onTouchStart(event);
    });

    window.document.addEventListener('touchend', (event) => {
      gestures.onTouchEnd(event);
    });
  },
  false,
);
