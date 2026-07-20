export function requireNotNull<T>(
  value: T | null | undefined,
  message: string,
): T {
  if (value == null) {
    throw new Error('Expression ' + message + ' evaluated to null.');
  }
  return value;
}
