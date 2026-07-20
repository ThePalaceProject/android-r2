/**
 * @jest-environment jsdom
 */

import { putSettings, SR2ColorScheme, SR2Font } from './settings';

test('settings (black on white, serif)', () => {
  putSettings({
    colorScheme: SR2ColorScheme.SR2_BLACK_ON_WHITE,
    font: SR2Font.SR2_FONT_SERIF,
    fontSizePercent: 100.0,
  });

  const root = document.documentElement;
  const style = root.style;

  expect(style.getPropertyValue('--USER__advancedSettings')).toStrictEqual(
    'readium-advanced-on',
  );
  expect(style.getPropertyValue('--USER__appearance')).toStrictEqual(
    'readium-default-on',
  );
  expect(style.getPropertyValue('--USER__fontOverride')).toStrictEqual(
    'readium-font-on',
  );
  expect(style.getPropertyValue('--USER__fontFamily')).toStrictEqual('serif');
  expect(style.getPropertyValue('--USER__fontSize')).toStrictEqual('100%');
});

test('settings (black on white, sans serif)', () => {
  putSettings({
    colorScheme: SR2ColorScheme.SR2_BLACK_ON_WHITE,
    font: SR2Font.SR2_FONT_SANS_SERIF,
    fontSizePercent: 100.0,
  });

  const root = document.documentElement;
  const style = root.style;

  expect(style.getPropertyValue('--USER__advancedSettings')).toStrictEqual(
    'readium-advanced-on',
  );
  expect(style.getPropertyValue('--USER__appearance')).toStrictEqual(
    'readium-default-on',
  );
  expect(style.getPropertyValue('--USER__fontOverride')).toStrictEqual(
    'readium-font-on',
  );
  expect(style.getPropertyValue('--USER__fontFamily')).toStrictEqual(
    'sans-serif',
  );
  expect(style.getPropertyValue('--USER__fontSize')).toStrictEqual('100%');
});

test('settings (black on white, opendyslexic)', () => {
  putSettings({
    colorScheme: SR2ColorScheme.SR2_BLACK_ON_WHITE,
    font: SR2Font.SR2_FONT_OPENDYSLEXIC,
    fontSizePercent: 100.0,
  });

  const root = document.documentElement;
  const style = root.style;

  expect(style.getPropertyValue('--USER__advancedSettings')).toStrictEqual(
    'readium-advanced-on',
  );
  expect(style.getPropertyValue('--USER__appearance')).toStrictEqual(
    'readium-default-on',
  );
  expect(style.getPropertyValue('--USER__fontOverride')).toStrictEqual(
    'readium-font-on',
  );
  expect(style.getPropertyValue('--USER__fontFamily')).toStrictEqual(
    'OpenDyslexic',
  );
  expect(style.getPropertyValue('--USER__fontSize')).toStrictEqual('100%');
});

test('settings (white on black, serif)', () => {
  putSettings({
    colorScheme: SR2ColorScheme.SR2_WHITE_ON_BLACK,
    font: SR2Font.SR2_FONT_SERIF,
    fontSizePercent: 100.0,
  });

  const root = document.documentElement;
  const style = root.style;

  expect(style.getPropertyValue('--USER__advancedSettings')).toStrictEqual(
    'readium-advanced-on',
  );
  expect(style.getPropertyValue('--USER__appearance')).toStrictEqual(
    'readium-night-on',
  );
  expect(style.getPropertyValue('--USER__fontOverride')).toStrictEqual(
    'readium-font-on',
  );
  expect(style.getPropertyValue('--USER__fontFamily')).toStrictEqual('serif');
  expect(style.getPropertyValue('--USER__fontSize')).toStrictEqual('100%');
});

test('settings (black on sepia, serif)', () => {
  putSettings({
    colorScheme: SR2ColorScheme.SR2_BLACK_ON_SEPIA,
    font: SR2Font.SR2_FONT_SERIF,
    fontSizePercent: 100.0,
  });

  const root = document.documentElement;
  const style = root.style;

  expect(style.getPropertyValue('--USER__advancedSettings')).toStrictEqual(
    'readium-advanced-on',
  );
  expect(style.getPropertyValue('--USER__appearance')).toStrictEqual(
    'readium-sepia-on',
  );
  expect(style.getPropertyValue('--USER__fontOverride')).toStrictEqual(
    'readium-font-on',
  );
  expect(style.getPropertyValue('--USER__fontFamily')).toStrictEqual('serif');
  expect(style.getPropertyValue('--USER__fontSize')).toStrictEqual('100%');
});

test('settings (white on black, publisher)', () => {
  putSettings({
    colorScheme: SR2ColorScheme.SR2_WHITE_ON_BLACK,
    font: SR2Font.SR2_FONT_PUBLISHER,
    fontSizePercent: 100.0,
  });

  const root = document.documentElement;
  const style = root.style;

  expect(style.getPropertyValue('--USER__advancedSettings')).toStrictEqual('');
  expect(style.getPropertyValue('--USER__appearance')).toStrictEqual(
    'readium-night-on',
  );
  expect(style.getPropertyValue('--USER__fontOverride')).toStrictEqual('');
  expect(style.getPropertyValue('--USER__fontFamily')).toStrictEqual('');
  expect(style.getPropertyValue('--USER__fontSize')).toStrictEqual('100%');
});
