import { requireDefined } from './notnull';

describe('requireNotNull', () => {
  it('disallows null', () => {
    expect(() => requireDefined(null, 'x')).toThrow();
  });
  it('returns the first argument', () => {
    expect(requireDefined('z', 'x')).toBe('z');
  });
});
