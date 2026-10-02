import { describe, expect, it } from 'vitest';

import { ChartSpec, Palette, buildConfig } from './chart-config';

const PALETTE: Palette = {
  ink: '#111',
  ink2: '#222',
  ink3: '#333',
  line: '#ddd',
  surface: '#fff',
  font: 'Mona Sans',
  colours: {
    'series-1': '#2a78d6',
    'series-2': '#eb6834',
    'series-3': '#1baf7a',
    'series-4': '#eda100',
    'status-warning': '#fab219',
    'status-critical': '#d03b3b',
  },
};

const LINE: ChartSpec = {
  type: 'line',
  labels: ['1 Oct', '2 Oct'],
  series: [{ label: 'Sign-ins', data: [3, 5], colour: 'series-1' }],
};

// The configuration is loosely typed by Chart.js; reading it back needs a cast.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const options = (spec: ChartSpec, locale = 'en-GB', animate = true): any =>
  buildConfig(spec, PALETTE, locale, animate).options;

describe('buildConfig', () => {
  it('paints each series in its slot of the palette', () => {
    const config = buildConfig(LINE, PALETTE, 'en-GB', true);

    expect(config.data.datasets[0].borderColor).toBe('#2a78d6');
    expect(config.data.labels).toEqual(['1 Oct', '2 Oct']);
  });

  it('shows no legend for a single series, whose title names it', () => {
    expect(options(LINE).plugins.legend.display).toBe(false);

    const two: ChartSpec = {
      ...LINE,
      series: [...LINE.series, { label: 'Other', data: [1, 1], colour: 'series-2' }],
    };
    expect(options(two).plugins.legend.display).toBe(true);
  });

  it('keeps still for a reader who asked for less motion', () => {
    expect(options(LINE, 'en-GB', false).animation).toBe(false);
  });

  it('writes the value axis in the reader’s number format', () => {
    const tick = options(LINE, 'de-DE').scales.y.ticks.callback;

    expect(tick(1234)).toBe('1.234');
  });

  it('lays a ranking along the y axis, with the values on x', () => {
    const scales = options({ ...LINE, type: 'bar', horizontal: true }).scales;

    expect(scales.x.ticks.callback(1500)).toBe('1,500');
    expect(scales.y.ticks.callback).toBeUndefined();
  });

  it('gives a doughnut one colour per slice and no axes', () => {
    const spec: ChartSpec = {
      type: 'doughnut',
      labels: ['On trial', 'Subscribed'],
      series: [{ label: 'Owners', data: [2, 3], colour: 'series-1' }],
      sliceColours: ['series-1', 'series-2'],
    };
    const config = buildConfig(spec, PALETTE, 'en-GB', true);

    expect(config.data.datasets[0].backgroundColor).toEqual(['#2a78d6', '#eb6834']);
    expect(options(spec).scales).toEqual({});
  });
});
