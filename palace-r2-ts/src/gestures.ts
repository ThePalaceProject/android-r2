import { requireNotNull } from './notnull';

/**
 * The API exposed by the gesture handler.
 */

export interface SR2GesturesType {
  /**
   * A starting touch event was received.
   */

  onTouchStart(event: TouchEvent): void;

  /**
   * An ending touch event was received.
   */

  onTouchEnd(event: TouchEvent): void;
}

export interface SR2GestureParametersType {
  window: Window;
  onTapLeft: () => void;
  onTapRight: () => void;
  onSwipeLeft: () => void;
  onSwipeRight: () => void;
}

export class SR2Gestures implements SR2GesturesType {
  private readonly availWidth: number;
  private readonly availHeight: number;
  private startX: number = 0;
  private startY: number = 0;
  private timeStart: number = Date.now();
  private singleTouch: boolean = false;
  private readonly onTapLeft: () => void;
  private readonly onTapRight: () => void;
  private readonly onSwipeLeft: () => void;
  private readonly onSwipeRight: () => void;

  private constructor(
    availWidth: number,
    availHeight: number,
    parameters: SR2GestureParametersType,
  ) {
    this.availWidth = requireNotNull(availWidth, 'AvailWidth');
    this.availHeight = requireNotNull(availHeight, 'AvailHeight');
    this.onTapLeft = requireNotNull(parameters.onTapLeft, 'OnTapLeft');
    this.onTapRight = requireNotNull(parameters.onTapRight, 'OnTapRight');
    this.onSwipeLeft = requireNotNull(parameters.onSwipeLeft, 'OnSwipeLeft');
    this.onSwipeRight = requireNotNull(parameters.onSwipeRight, 'OnSwipeRight');
  }

  onTouchStart(event: TouchEvent): void {
    this.timeStart = Date.now();

    const target: EventTarget | null = event.target;
    if (target instanceof Element) {
      if (target.nodeName.toUpperCase() === 'A') {
        this.singleTouch = false;
        return;
      }
    }

    this.singleTouch = event.touches.length == 1;
    const touch: Touch | undefined = event.changedTouches[0];
    if (!touch) {
      return;
    }

    this.startX = touch.screenX % this.availWidth;
    this.startY = touch.screenY % this.availHeight;
  }

  onTouchEnd(event: TouchEvent): void {
    const timeEnd = Date.now();
    const timeDelta = timeEnd - this.timeStart;

    if (!this.singleTouch) {
      return;
    }

    const touch = event.changedTouches[0];
    if (!touch) {
      return;
    }

    const relativeDistanceX = Math.abs(
      ((touch.screenX % this.availWidth) - this.startX) / this.availWidth,
    );
    const relativeDistanceY = Math.abs(
      ((touch.screenY % this.availHeight) - this.startY) / this.availHeight,
    );
    const touchDistance = Math.max(relativeDistanceX, relativeDistanceY);

    const swipeFarEnough = relativeDistanceX > 0.25;
    const swipeFastEnough = timeDelta < 250;
    if (swipeFastEnough && swipeFarEnough) {
      this.onSwipe(event);
      return;
    }

    const tapAreaSize = 0.01;
    if (touchDistance < tapAreaSize) {
      this.onPageMovementTap(event, touch);
      return;
    }
  }

  private onPageMovementTap(event: TouchEvent, touch: Touch) {
    const position = (touch.screenX % this.availWidth) / this.availWidth;

    if (position <= 0.2) {
      this.onTapLeft();
    } else if (position >= 0.8) {
      this.onTapRight();
    }

    event.stopPropagation();
    event.preventDefault();
  }

  private onSwipe(event: TouchEvent) {
    const touch = event.changedTouches[0];

    if (!touch) {
      return;
    }

    const direction =
      ((touch.screenX % this.availWidth) - this.startX) / this.availWidth;

    if (direction > 0) {
      this.onSwipeRight();
    } else {
      this.onSwipeLeft();
    }

    event.stopPropagation();
    event.preventDefault();
  }

  public static create(parameters: SR2GestureParametersType): SR2GesturesType {
    const availWidth = window.screen.availWidth;
    const availHeight = window.screen.availHeight;

    return new SR2Gestures(availWidth, availHeight, parameters);
  }
}
