/**
 * Make MathML elements readable by screen readers.
 *
 * The browser renders MathML natively, but a screen reader reads the raw
 * tokens (`m s u p m i x m n 2 ...`) rather than the equation. For each
 * math element we compute a speech string with the Speech Rule Engine and
 * replace the element's accessibility presence with an image that carries
 * the speech string as its label. The visual rendering is untouched.
 */

import { requireDefined } from './notnull';

/** The subset of the SRE API used by this module. */

export interface SR2SREType {
  /** Resolves when the engine has finished loading its rules. */
  engineReady: () => Promise<unknown>;
  /** Computes the speech string for a MathML document fragment. */
  toSpeech: (mathml: string) => string;
}

declare global {
  interface Window {
    SRE?: SR2SREType;
  }
}

const XHTML_NAMESPACE = 'http://www.w3.org/1999/xhtml';

/** The attribute marking a label span created by this module. */

const LABEL_MARKER = 'data-sr2-speech';

/**
 * Compute the speech annotation for every math element in the document.
 *
 * Does nothing if the SRE script was not injected into this chapter.
 * The returned promise resolves once every element has been annotated.
 */

export function mathMakeAccessible(): Promise<void> {
  const sre = window.SRE;
  if (sre === undefined) {
    return Promise.resolve();
  }

  return sre.engineReady().then(() => {
    const elements = document.querySelectorAll('math');
    let annotated = 0;

    for (const element of elements) {
      if (annotateMath(sre, element)) {
        annotated += 1;
      }
    }

    console.log(
      `SR2 math: annotated ${annotated.toString()} of ${elements.length.toString()} math elements`,
    );
  });
}

/**
 * Annotate a single math element, hiding it from the accessibility tree
 * and wrapping it in a labelled image element.
 *
 * The wrapper is in normal flow, so its box coincides with the box of the
 * rendered equation. Screen readers that draw a focus highlight around the
 * spoken element (TalkBack does) therefore highlight the equation itself.
 */

function annotateMath(sre: SR2SREType, math: Element): boolean {
  const parent = math.parentNode;
  if (parent === null) {
    return false;
  }

  if (math.parentElement?.hasAttribute(LABEL_MARKER) === true) {
    return false;
  }

  let speech: string;
  try {
    speech = sre.toSpeech(mathmlToFragment(math));
  } catch (error) {
    console.warn(`SR2 math: SRE failed on element: ${String(error)}`);
    return false;
  }

  if (speech === '') {
    return false;
  }

  math.setAttribute('aria-hidden', 'true');

  const label = document.createElementNS(XHTML_NAMESPACE, 'span');
  label.setAttribute('role', 'img');
  label.setAttribute('aria-label', `${speech}, math`);
  label.setAttribute(LABEL_MARKER, '');
  if (math.getAttribute('display') === 'block') {
    label.setAttribute('style', 'display:block');
  }

  parent.insertBefore(label, math);
  label.appendChild(math);

  return true;
}

/**
 * Serialize a math element to a standalone MathML fragment that SRE can
 * parse.
 *
 * EPUBs commonly namespace their math elements (e.g. `<m:math>`), but a
 * serialized fragment does not carry the namespace declaration that binds
 * the prefix. SRE parses the fragment in isolation, where the prefix would
 * be unbound and the parse would fail. The element is therefore rebuilt in
 * a detached tree without prefixes, and the result serialized.
 */

export function mathmlToFragment(math: Element): string {
  const rebuilt = rebuildWithoutPrefix(math);
  return new XMLSerializer().serializeToString(rebuilt);
}

/**
 * Deep-copy an element into a detached tree, dropping namespace prefixes
 * but keeping namespaces and local names.
 */

function rebuildWithoutPrefix(element: Element): Element {
  const clone = document.createElementNS(
    requireDefined(element.namespaceURI, 'Element namespace'),
    element.localName,
  );

  for (const attribute of element.attributes) {
    clone.setAttributeNS(
      attribute.namespaceURI,
      attribute.name,
      attribute.value,
    );
  }

  for (const child of Array.from(element.childNodes)) {
    if (child instanceof Element) {
      clone.appendChild(rebuildWithoutPrefix(child));
    } else if (child.nodeType === Node.TEXT_NODE) {
      clone.appendChild(document.createTextNode(child.textContent ?? ''));
    }
  }

  return clone;
}
