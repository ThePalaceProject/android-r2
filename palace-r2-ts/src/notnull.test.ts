import { requireNotNull } from './notnull';

describe('requireNotNull', () => {
  it('disallows null', () => {
    expect(() => requireNotNull(null, 'x')).toThrow();
  });
  it('returns the first argument', () => {
    expect(requireNotNull('z', 'x')).toBe('z');
  });
});
