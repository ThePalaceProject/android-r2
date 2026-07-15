import { SR2Page, SR2PageSet, SR2PageSetStatus } from './pageset';

test('page constructor asserts range', () => {
  expect(() => {
    new SR2Page(0, -1.0, 100);
  }).toThrow();
});

test('page constructor asserts range', () => {
  expect(() => {
    new SR2Page(0, 1.1, 100);
  }).toThrow();
});

test('empty controller has one page', () => {
  const c = SR2PageSet.create();
  expect(c.pageCount()).toStrictEqual(1);
  expect(c.statusNow()).toStrictEqual({ kind: 'Initial' });
});

test('recomputing the controller publishes status values', () => {
  const c = SR2PageSet.create();
  const r: [SR2PageSetStatus, SR2PageSetStatus][] = [];
  const s = c.status.subscribe((oldV, newV) => {
    r.push([oldV, newV]);
  });

  expect(c.pageCount()).toStrictEqual(1);
  expect(c.statusNow()).toStrictEqual({ kind: 'Initial' });
  c.recompute(1000.0, 100.0);

  expect(r[0]?.[0]).toStrictEqual({ kind: 'Initial' });
  expect(r[0]?.[1]).toStrictEqual({ kind: 'Initial' });

  expect(r[1]?.[0]).toStrictEqual({ kind: 'Initial' });
  expect(r[1]?.[1]).toStrictEqual({ kind: 'CalculatingPages', progress: 0.0 });

  expect(r[2]?.[0]).toStrictEqual({ kind: 'CalculatingPages', progress: 0.0 });
  expect(r[2]?.[1]).toStrictEqual({ kind: 'CalculatingPages', progress: 0.0 });

  expect(r[3]?.[0]).toStrictEqual({ kind: 'CalculatingPages', progress: 0.0 });
  expect(r[3]?.[1]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.1111111111111111,
  });

  expect(r[4]?.[0]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.1111111111111111,
  });
  expect(r[4]?.[1]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.2222222222222222,
  });

  expect(r[5]?.[0]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.2222222222222222,
  });
  expect(r[5]?.[1]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.3333333333333333,
  });

  expect(r[6]?.[0]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.3333333333333333,
  });
  expect(r[6]?.[1]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.4444444444444444,
  });

  expect(r[7]?.[0]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.4444444444444444,
  });
  expect(r[7]?.[1]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.5555555555555556,
  });

  expect(r[8]?.[0]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.5555555555555556,
  });
  expect(r[8]?.[1]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.6666666666666666,
  });

  expect(r[9]?.[0]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.6666666666666666,
  });
  expect(r[9]?.[1]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.7777777777777778,
  });

  expect(r[10]?.[0]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.7777777777777778,
  });
  expect(r[10]?.[1]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.8888888888888888,
  });

  expect(r[11]?.[0]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 0.8888888888888888,
  });
  expect(r[11]?.[1]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 1.0,
  });

  expect(r[12]?.[0]).toStrictEqual({
    kind: 'CalculatingPages',
    progress: 1.0,
  });
  expect(r[12]?.[1]).toStrictEqual({ kind: 'Ready' });

  expect(r.length).toStrictEqual(13);

  s.unsubscribe();
});

test('finding pages works', () => {
  const c = SR2PageSet.create();

  {
    const p = c.findClosestPage(0.0);
    expect(p.index).toStrictEqual(0);
    expect(p.scrollOffset).toStrictEqual(0.0);
  }

  {
    const p = c.findClosestPage(1.0);
    expect(p.index).toStrictEqual(0);
    expect(p.scrollOffset).toStrictEqual(0.0);
  }

  c.recompute(1000.0, 100.0);
  expect(c.pageCount()).toStrictEqual(9);

  for (const p of c.pages()) {
    console.log('Page: ', p);
  }

  {
    const p = c.findClosestPage(0.0);
    expect(p.index).toStrictEqual(0);
    expect(p.scrollOffset).toStrictEqual(0.0);
  }

  {
    const p = c.findClosestPage(0.3);
    expect(p.index).toStrictEqual(2);
    expect(p.scrollOffset).toStrictEqual(0.2222222222222222);
    expect(p.scrollOffsetRaw).toStrictEqual(200);
  }

  {
    const p = c.findClosestPage(1.1);
    expect(p.index).toStrictEqual(8);
    expect(p.scrollOffset).toStrictEqual(0.8888888888888888);
    expect(p.scrollOffsetRaw).toStrictEqual(800);
  }
});
