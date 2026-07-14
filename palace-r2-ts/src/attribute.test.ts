import { Attribute } from './attribute';

test('attribute value received on subscribe', () => {
  const a = Attribute.create(23);
  const r: [number, number][] = [];
  const s = a.subscribe((oldV, newV) => {
    r.push([oldV, newV]);
  });
  expect(a.valueNow()).toStrictEqual(23);

  a.set(24);
  expect(a.valueNow()).toStrictEqual(24);

  a.set(25);
  expect(a.valueNow()).toStrictEqual(25);

  s.unsubscribe();
  a.set(26);
  expect(a.valueNow()).toStrictEqual(26);

  expect(r[0]).toStrictEqual([23, 23]);
  expect(r[1]).toStrictEqual([23, 24]);
  expect(r[2]).toStrictEqual([24, 25]);
  expect(r.length).toStrictEqual(3);
});

test('attribute value received on multiple subscribers', () => {
  const a = Attribute.create(23);
  const r: [number, number][] = [];
  const s0 = a.subscribe((oldV, newV) => {
    r.push([oldV, newV]);
  });
  const s1 = a.subscribe((oldV, newV) => {
    r.push([oldV, newV]);
  });
  expect(a.valueNow()).toStrictEqual(23);

  a.set(24);
  expect(a.valueNow()).toStrictEqual(24);
  s1.unsubscribe();

  a.set(25);
  expect(a.valueNow()).toStrictEqual(25);

  s0.unsubscribe();
  a.set(26);
  expect(a.valueNow()).toStrictEqual(26);

  expect(r[0]).toStrictEqual([23, 23]);
  expect(r[1]).toStrictEqual([23, 23]);
  expect(r[2]).toStrictEqual([23, 24]);
  expect(r[3]).toStrictEqual([23, 24]);
  expect(r[4]).toStrictEqual([24, 25]);
  expect(r.length).toStrictEqual(5);
});
