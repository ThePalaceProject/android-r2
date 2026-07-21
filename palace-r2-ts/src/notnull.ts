export function requireDefined<T>(
  value: T | null | undefined,
  message: string,
): T {
  if (value === null || value === undefined) {
    throw new Error(
      'Expression ' + message + ' evaluated to null or undefined.',
    );
  }
  return value;
}
