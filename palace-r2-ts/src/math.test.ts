/**
 * @jest-environment jsdom
 */

import { mathMakeAccessible, mathmlToFragment, SR2SREType } from './math';

const MATHML = 'http://www.w3.org/1998/Math/MathML';

beforeEach(() => {
  document.body.innerHTML = '';
  delete window.SRE;
});

/**
 * Build a MathML element of the form `x^2`. When prefixed, the element
 * names are prefixed with `m:`, as in EPUBs that namespace their math.
 */

function createMath(prefixed: boolean): Element {
  const name = (local: string): string => (prefixed ? `m:${local}` : local);

  const math = document.createElementNS(MATHML, name('math'));
  const msup = document.createElementNS(MATHML, name('msup'));
  const mi = document.createElementNS(MATHML, name('mi'));
  mi.textContent = 'x';
  const mn = document.createElementNS(MATHML, name('mn'));
  mn.textContent = '2';
  msup.appendChild(mi);
  msup.appendChild(mn);
  math.appendChild(msup);

  return math;
}

test('mathmlToFragment (prefixed) is a standalone fragment', () => {
  const math = createMath(true);

  const fragment = mathmlToFragment(math);

  expect(fragment).not.toContain('m:');
  expect(fragment).toContain('xmlns="http://www.w3.org/1998/Math/MathML"');
  expect(fragment).toContain('<msup><mi>x</mi><mn>2</mn></msup>');
});

test('mathmlToFragment (unprefixed) is a standalone fragment', () => {
  const math = createMath(false);

  const fragment = mathmlToFragment(math);

  expect(fragment).toContain('<msup><mi>x</mi><mn>2</mn></msup>');
});

test('mathmlToFragment (element with attribute) keeps the attribute', () => {
  const math = createMath(true);
  math.setAttribute('aria-hidden', 'true');

  const fragment = mathmlToFragment(math);

  expect(fragment).toContain('aria-hidden="true"');
  expect(fragment).not.toContain('m:');
});

test('math (no SRE script) does nothing', async () => {
  document.body.appendChild(createMath(false));

  await mathMakeAccessible();

  const math = document.querySelector('math');
  expect(math).not.toBeNull();
  expect(math?.getAttribute('aria-hidden')).toBeNull();
  expect(math?.nextElementSibling).toBeNull();
});

test('math (with SRE) is annotated', async () => {
  document.body.appendChild(createMath(false));

  const fragments: string[] = [];
  const sre: SR2SREType = {
    engineReady: () => Promise.resolve(),
    toSpeech: (mathml: string) => {
      fragments.push(mathml);
      return 'x squared equals y plus 2';
    },
  };
  window.SRE = sre;

  await mathMakeAccessible();

  const math = document.querySelector('math');
  expect(math).not.toBeNull();
  expect(math?.getAttribute('aria-hidden')).toBe('true');

  // The label span wraps the math element.
  const label = math?.parentElement;
  expect(label).not.toBeNull();
  expect(label?.localName).toBe('span');
  expect(label?.getAttribute('role')).toBe('img');
  expect(label?.getAttribute('aria-label')).toBe(
    'x squared equals y plus 2, math',
  );
  // Inline math: the wrapper is an unstyled inline span.
  expect(label?.getAttribute('style')).toBeNull();

  // The fragment handed to SRE is standalone.
  expect(fragments).toHaveLength(1);
  expect(fragments[0]).toContain('<msup><mi>x</mi><mn>2</mn></msup>');

  // A second pass is a no-op.
  await mathMakeAccessible();
  expect(fragments).toHaveLength(1);
  expect(math?.parentElement?.localName).toBe('span');
});

test('math (display=block) gets a block wrapper', async () => {
  const math = createMath(false);
  math.setAttribute('display', 'block');
  document.body.appendChild(math);

  const sre: SR2SREType = {
    engineReady: () => Promise.resolve(),
    toSpeech: () => 'x squared',
  };
  window.SRE = sre;

  await mathMakeAccessible();

  expect(math.parentElement?.localName).toBe('span');
  expect(math.parentElement?.getAttribute('style')).toBe('display:block');
});

test('math (SRE failure) is left untouched', async () => {
  document.body.appendChild(createMath(false));

  const sre: SR2SREType = {
    engineReady: () => Promise.resolve(),
    toSpeech: () => {
      throw new Error(
        'NamespaceError: prefix is non-null and namespace is null',
      );
    },
  };
  window.SRE = sre;

  await mathMakeAccessible();

  const math = document.querySelector('math');
  expect(math).not.toBeNull();
  expect(math?.getAttribute('aria-hidden')).toBeNull();
  expect(math?.nextElementSibling).toBeNull();
});
