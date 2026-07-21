import { requireDefined } from './notnull';
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
  expect(c.pageCount()).toStrictEqual(10);

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
    expect(p.index).toStrictEqual(9);
    expect(p.scrollOffset).toStrictEqual(1.0);
    expect(p.scrollOffsetRaw).toStrictEqual(900);
  }
});

test('computing a single page works', () => {
  const c = SR2PageSet.create();
  c.recompute(1.0, 1.0);
  expect(c.pageCount()).toStrictEqual(1);

  for (const p of c.pages()) {
    console.log('Page: ', p);
  }

  {
    const p0 = c.pages()[0];
    expect(p0).toStrictEqual(new SR2Page(0, 0, 0));
  }
});

test('next/previous page works', () => {
  const c = SR2PageSet.create();
  c.recompute(10.0, 3.0);
  expect(c.pageCount()).toStrictEqual(4);

  {
    const p0 = requireDefined(c.pages()[0], 'p0');
    const p1 = requireDefined(c.pageNext(p0), 'p1');
    const p2 = requireDefined(c.pageNext(p1), 'p2');
    const p3 = requireDefined(c.pageNext(p2), 'p3');
    const p4 = c.pageNext(p3);
    expect(p0.index).toStrictEqual(0);
    expect(p1.index).toStrictEqual(1);
    expect(p2.index).toStrictEqual(2);
    expect(p3.index).toStrictEqual(3);
    expect(p4).toBeNull();

    const q2 = requireDefined(c.pagePrevious(p3), 'q2');
    expect(q2).toStrictEqual(p2);
    const q1 = requireDefined(c.pagePrevious(q2), 'q1');
    expect(q1).toStrictEqual(p1);
    const q0 = requireDefined(c.pagePrevious(q1), 'q0');
    expect(q0).toStrictEqual(p0);
    const qk0 = c.pagePrevious(q0);
    expect(qk0).toBeNull();
  }
});

test('last page is included when document does not divide evenly', () => {
  const c = SR2PageSet.create();
  c.recompute(1000.0, 300.0);

  expect(c.pageCount()).toStrictEqual(4);

  const pages = c.pages();
  expect(pages[0]?.scrollOffsetRaw).toStrictEqual(0);
  expect(pages[1]?.scrollOffsetRaw).toStrictEqual(300);
  expect(pages[2]?.scrollOffsetRaw).toStrictEqual(600);
  expect(pages[3]?.scrollOffsetRaw).toStrictEqual(700);

  expect(pages[0]?.scrollOffset).toStrictEqual(0.0);
  expect(pages[3]?.scrollOffset).toStrictEqual(1.0);
});

test('no phantom last page when loop already covers document', () => {
  const c = SR2PageSet.create();
  c.recompute(3606, 600.9389348488247);

  expect(c.pageCount()).toStrictEqual(6);

  const pages = c.pages();
  const last = requireDefined(pages[5], 'last');
  const penultimate = requireDefined(pages[4], 'penultimate');

  expect(last.scrollOffsetRaw - penultimate.scrollOffsetRaw).toBeGreaterThan(1);
});

test('single page chapter produces one page with offset zero', () => {
  const c = SR2PageSet.create();
  c.recompute(1.0, 1.0);

  expect(c.pageCount()).toStrictEqual(1);
  const p0 = requireDefined(c.pages()[0], 'p0');
  expect(p0.scrollOffset).toStrictEqual(0.0);
  expect(p0.scrollOffsetRaw).toStrictEqual(0);
});
