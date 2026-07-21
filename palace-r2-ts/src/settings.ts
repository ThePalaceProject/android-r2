import { requireDefined } from './notnull';
import { unreachable } from './unreachable';

/**
 * The reader color scheme.
 */

export enum SR2ColorScheme {
  /** White text on a black background. */
  SR2_WHITE_ON_BLACK = 'SR2_WHITE_ON_BLACK',
  /** Black text on a white background. */
  SR2_BLACK_ON_WHITE = 'SR2_BLACK_ON_WHITE',
  /** Black text on a sepia background. */
  SR2_BLACK_ON_SEPIA = 'SR2_BLACK_ON_SEPIA',
}

/** The reader font family. */

export enum SR2Font {
  /** The standard serif font. */
  SR2_FONT_SERIF = 'SR2_FONT_SERIF',
  /** The standard sans-serif font. */
  SR2_FONT_SANS_SERIF = 'SR2_FONT_SANS_SERIF',
  /** The OpenDyslexic font. */
  SR2_FONT_OPENDYSLEXIC = 'SR2_FONT_OPENDYSLEXIC',
  /** The publisher CSS default. */
  SR2_FONT_PUBLISHER = 'SR2_FONT_PUBLISHER',
}

/** The type of settings. */

export interface SR2SettingsType {
  colorScheme: SR2ColorScheme;
  font: SR2Font;
  fontSizePercent: number;
}

/**
 * Write the given settings to the document.
 */

export function putSettings(settings: SR2SettingsType) {
  requireDefined(settings, 'Settings');

  const root = document.documentElement;

  const colorScheme = settings.colorScheme;
  switch (colorScheme) {
    case SR2ColorScheme.SR2_WHITE_ON_BLACK: {
      root.style.setProperty('--USER__appearance', 'readium-night-on');
      break;
    }
    case SR2ColorScheme.SR2_BLACK_ON_WHITE: {
      root.style.setProperty('--USER__appearance', 'readium-default-on');
      break;
    }
    case SR2ColorScheme.SR2_BLACK_ON_SEPIA: {
      root.style.setProperty('--USER__appearance', 'readium-sepia-on');
      break;
    }
    default: {
      unreachable(colorScheme);
    }
  }

  const font = settings.font;
  switch (font) {
    case SR2Font.SR2_FONT_SERIF: {
      root.style.setProperty('--USER__advancedSettings', 'readium-advanced-on');
      root.style.setProperty('--USER__fontOverride', 'readium-font-on');
      root.style.setProperty('--USER__fontFamily', 'serif');
      break;
    }
    case SR2Font.SR2_FONT_SANS_SERIF: {
      root.style.setProperty('--USER__advancedSettings', 'readium-advanced-on');
      root.style.setProperty('--USER__fontOverride', 'readium-font-on');
      root.style.setProperty('--USER__fontFamily', 'sans-serif');
      break;
    }
    case SR2Font.SR2_FONT_OPENDYSLEXIC: {
      root.style.setProperty('--USER__advancedSettings', 'readium-advanced-on');
      root.style.setProperty('--USER__fontOverride', 'readium-font-on');
      root.style.setProperty('--USER__fontFamily', 'OpenDyslexic');
      break;
    }
    case SR2Font.SR2_FONT_PUBLISHER: {
      root.style.setProperty('--USER__advancedSettings', '');
      root.style.setProperty('--USER__fontOverride', '');
      root.style.removeProperty('--USER__fontFamily');
      break;
    }
    default: {
      unreachable(font);
    }
  }

  const percentText = String(settings.fontSizePercent) + '%';
  root.style.setProperty('--USER__fontSize', percentText);
}
