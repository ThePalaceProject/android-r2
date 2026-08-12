import { SR2Gestures } from './gestures';

class MockElement {
  constructor(public nodeName = '') {}
}
(globalThis as unknown as Record<string, unknown>)['Element'] = MockElement;

function setupMockWindow(availWidth = 1000, availHeight = 1000) {
  (globalThis as unknown as Record<string, unknown>)['window'] = {
    screen: { availWidth, availHeight },
  };
}

function makeTouchEvent(
  touchX: number,
  touchY: number,
  options?: { touchCount?: number },
): TouchEvent {
  const touch = { screenX: touchX, screenY: touchY };
  const count = options?.touchCount ?? 1;
  const touches = Array.from({ length: count }, () => touch);
  return {
    touches,
    changedTouches: [touch],
    stopPropagation: () => {
      // Nothing required!
    },
    preventDefault: () => {
      // Nothing required!
    },
  } as unknown as TouchEvent;
}

function makeParams() {
  const onTapLeft = jest.fn();
  const onTapRight = jest.fn();
  const onSwipeLeft = jest.fn();
  const onSwipeRight = jest.fn();

  const gestures = SR2Gestures.create({
    window: {} as Window,
    onTapLeft,
    onTapRight,
    onSwipeLeft,
    onSwipeRight,
  });

  return { gestures, onTapLeft, onTapRight, onSwipeLeft, onSwipeRight };
}

test('tap on left third triggers onTapLeft', () => {
  setupMockWindow();
  const { gestures, onTapLeft, onTapRight, onSwipeLeft, onSwipeRight } =
    makeParams();

  gestures.onTouchStart(makeTouchEvent(100, 500));
  gestures.onTouchEnd(makeTouchEvent(100, 500));

  expect(onTapLeft).toHaveBeenCalledTimes(1);
  expect(onTapRight).not.toHaveBeenCalled();
  expect(onSwipeLeft).not.toHaveBeenCalled();
  expect(onSwipeRight).not.toHaveBeenCalled();
});

test('tap on right third triggers onTapRight', () => {
  setupMockWindow();
  const { gestures, onTapLeft, onTapRight, onSwipeLeft, onSwipeRight } =
    makeParams();

  gestures.onTouchStart(makeTouchEvent(900, 500));
  gestures.onTouchEnd(makeTouchEvent(900, 500));

  expect(onTapRight).toHaveBeenCalledTimes(1);
  expect(onTapLeft).not.toHaveBeenCalled();
  expect(onSwipeLeft).not.toHaveBeenCalled();
  expect(onSwipeRight).not.toHaveBeenCalled();
});

test('tap in middle third triggers no callback', () => {
  setupMockWindow();
  const { gestures, onTapLeft, onTapRight, onSwipeLeft, onSwipeRight } =
    makeParams();

  gestures.onTouchStart(makeTouchEvent(500, 500));
  gestures.onTouchEnd(makeTouchEvent(500, 500));

  expect(onTapLeft).not.toHaveBeenCalled();
  expect(onTapRight).not.toHaveBeenCalled();
  expect(onSwipeLeft).not.toHaveBeenCalled();
  expect(onSwipeRight).not.toHaveBeenCalled();
});

test('fast swipe right triggers onSwipeRight', () => {
  setupMockWindow();
  const { gestures, onTapLeft, onTapRight, onSwipeLeft, onSwipeRight } =
    makeParams();

  gestures.onTouchStart(makeTouchEvent(100, 500));
  gestures.onTouchEnd(makeTouchEvent(900, 500));

  expect(onSwipeRight).toHaveBeenCalledTimes(1);
  expect(onSwipeLeft).not.toHaveBeenCalled();
  expect(onTapLeft).not.toHaveBeenCalled();
  expect(onTapRight).not.toHaveBeenCalled();
});

test('fast swipe left triggers onSwipeLeft', () => {
  setupMockWindow();
  const { gestures, onTapLeft, onTapRight, onSwipeLeft, onSwipeRight } =
    makeParams();

  gestures.onTouchStart(makeTouchEvent(900, 500));
  gestures.onTouchEnd(makeTouchEvent(100, 500));

  expect(onSwipeLeft).toHaveBeenCalledTimes(1);
  expect(onSwipeRight).not.toHaveBeenCalled();
  expect(onTapLeft).not.toHaveBeenCalled();
  expect(onTapRight).not.toHaveBeenCalled();
});

test('small horizontal movement below swipe threshold treated as tap', () => {
  setupMockWindow();
  const { gestures, onTapLeft, onSwipeLeft, onSwipeRight } = makeParams();

  // 5px on 1000px width = 0.005, below tapAreaSize (0.01) so it's a tap
  gestures.onTouchStart(makeTouchEvent(100, 500));
  gestures.onTouchEnd(makeTouchEvent(105, 500));

  // End position (105/1000 = 0.105) is in left tap zone
  expect(onTapLeft).toHaveBeenCalledTimes(1);
  expect(onSwipeLeft).not.toHaveBeenCalled();
  expect(onSwipeRight).not.toHaveBeenCalled();
});

test('vertical swipe alone does not trigger horizontal callbacks', () => {
  setupMockWindow();
  const { gestures, onTapLeft, onTapRight, onSwipeLeft, onSwipeRight } =
    makeParams();

  gestures.onTouchStart(makeTouchEvent(500, 100));
  gestures.onTouchEnd(makeTouchEvent(500, 800));

  expect(onTapLeft).not.toHaveBeenCalled();
  expect(onTapRight).not.toHaveBeenCalled();
  expect(onSwipeLeft).not.toHaveBeenCalled();
  expect(onSwipeRight).not.toHaveBeenCalled();
});

test('multi-touch is ignored (singleTouch flag prevents handling)', () => {
  setupMockWindow();
  const { gestures, onTapLeft, onTapRight, onSwipeLeft, onSwipeRight } =
    makeParams();

  gestures.onTouchStart(makeTouchEvent(100, 500, { touchCount: 2 }));
  gestures.onTouchEnd(makeTouchEvent(100, 500));

  expect(onTapLeft).not.toHaveBeenCalled();
  expect(onTapRight).not.toHaveBeenCalled();
  expect(onSwipeLeft).not.toHaveBeenCalled();
  expect(onSwipeRight).not.toHaveBeenCalled();
});

test('touch starting on an <a> element is ignored', () => {
  setupMockWindow();
  const { gestures, onTapLeft, onTapRight, onSwipeLeft, onSwipeRight } =
    makeParams();

  // Mock an <a> element (node environment has no DOM)
  const link = new MockElement('A') as unknown as Element;
  const event = {
    target: link,
    touches: [{ screenX: 100, screenY: 500 }],
    changedTouches: [{ screenX: 100, screenY: 500 }],
    stopPropagation: () => {
      // Nothing required.
    },
    preventDefault: () => {
      // Nothing required.
    },
  } as unknown as TouchEvent;

  gestures.onTouchStart(event);
  gestures.onTouchEnd(makeTouchEvent(100, 500));

  expect(onTapLeft).not.toHaveBeenCalled();
  expect(onTapRight).not.toHaveBeenCalled();
  expect(onSwipeLeft).not.toHaveBeenCalled();
  expect(onSwipeRight).not.toHaveBeenCalled();
});

test('screen coordinates wrap using availWidth', () => {
  // Simulate a 1000px-wide display on a multi-monitor setup
  // where the physical screen X starts at 1920
  setupMockWindow(1000, 1000);
  const { gestures, onTapLeft, onTapRight } = makeParams();

  // screenX = 1950 → 1950 % 1000 = 950 (right third → onTapRight)
  gestures.onTouchStart(makeTouchEvent(1950, 500));
  gestures.onTouchEnd(makeTouchEvent(1950, 500));

  expect(onTapRight).toHaveBeenCalledTimes(1);
  expect(onTapLeft).not.toHaveBeenCalled();
});

test('onMouseDown and onMouseUp do not exist (current limitation)', () => {
  setupMockWindow();
  const { gestures } = makeParams();

  // SR2Gestures has no mouse-handling methods.
  // On Chromebooks, clicks and trackpad swipes fire mouse events,
  // not touch events, so this gap causes the reader to be unresponsive.
  expect(
    (gestures as unknown as Record<string, unknown>)['onMouseDown'],
  ).toBeUndefined();
  expect(
    (gestures as unknown as Record<string, unknown>)['onMouseUp'],
  ).toBeUndefined();
});

test('passing a MouseEvent-like object to onTouchStart produces no callbacks', () => {
  setupMockWindow();
  const { gestures, onTapLeft, onTapRight, onSwipeLeft, onSwipeRight } =
    makeParams();

  // A MouseEvent-like object: has screenX/screenY but no changedTouches.
  // onTouchStart reads changedTouches[0] which is undefined, so early-returns.
  const mouseEvent = {
    screenX: 100,
    screenY: 500,
    // No changedTouches array
    touches: [],
    changedTouches: [],
    stopPropagation: () => {
      // Nothing required.
    },
    preventDefault: () => {
      // Nothing required.
    },
  } as unknown as TouchEvent;

  gestures.onTouchStart(mouseEvent);
  gestures.onTouchEnd(mouseEvent);

  expect(onTapLeft).not.toHaveBeenCalled();
  expect(onTapRight).not.toHaveBeenCalled();
  expect(onSwipeLeft).not.toHaveBeenCalled();
  expect(onSwipeRight).not.toHaveBeenCalled();
});
