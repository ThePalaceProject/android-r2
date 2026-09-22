import { requireDefined } from './notnull';

const easeInQuad = (t: number) => t * t;

export function smoothScrollTo(
  element: Element,
  target: number,
  duration: number,
) {
  requireDefined(element, 'element');
  requireDefined(target, 'target');
  requireDefined(duration, 'duration');

  const start = element.scrollLeft;
  const distance = target - start;
  const startTime = performance.now();

  function animate(currentTime: number) {
    const timeElapsed = currentTime - startTime;
    const progress = Math.min(timeElapsed / duration, 1);
    const easedProgress = easeInQuad(progress);
    const currentPosition = start + distance * easedProgress;

    element.scrollLeft = currentPosition;
    if (timeElapsed < duration) {
      requestAnimationFrame(animate);
    }
  }

  requestAnimationFrame(animate);
}
