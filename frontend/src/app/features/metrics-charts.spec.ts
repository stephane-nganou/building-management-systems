import { describe, expect, it } from 'vitest';

import { DayCount, Metrics } from '../core/models';
import { conversionRate, dayLabels, errorRate, panels } from './metrics-charts';

const DAYS = ['2026-09-30', '2026-10-01', '2026-10-02'];

function series(...counts: number[]): DayCount[] {
  return DAYS.map((day, index) => ({ day, count: counts[index] }));
}

const NONE = series(0, 0, 0);

const METRICS: Metrics = {
  days: 7,
  from: DAYS[0],
  to: DAYS[2],
  activity: {
    signIns: series(1, 0, 4),
    activeUsers: DAYS.map((day) => ({ day, owners: 2, assistants: 1 })),
    dau: { owners: 1, assistants: 1 },
    wau: { owners: 2, assistants: 1 },
    mau: { owners: 3, assistants: 2 },
  },
  customers: {
    trial: 2,
    active: 5,
    expired: 1,
    suspended: 0,
    newOwners: series(0, 1, 1),
    trialsStarted: 4,
    trialsConverted: 1,
  },
  features: {
    buildings: series(1, 0, 0),
    apartments: series(2, 2, 2),
    tenants: NONE,
    expenses: series(0, 0, 3),
    rentInvoices: NONE,
    coldWaterInvoices: NONE,
    pdfDownloads: NONE,
  },
  traffic: {
    requests: 200,
    clientErrors: 8,
    serverErrors: 2,
    perDay: DAYS.map((day) => ({ day, requests: 50, clientErrors: 2, serverErrors: 1 })),
    busiest: [],
    slowest: [],
  },
};

/** The keys stand in for their translations, which is enough to see where each went. */
const t = (key: string) => key;

describe('dayLabels', () => {
  it('writes a UTC day the way the reader does', () => {
    expect(dayLabels(['2026-10-02'], 'en-GB')).toEqual(['2 Oct']);
    expect(dayLabels(['2026-10-02'], 'de-DE')).toEqual(['2. Okt.']);
  });
});

describe('panels', () => {
  const drawn = panels(METRICS, t, 'en-GB');
  const panel = (title: string) => drawn.find((entry) => entry.title === title)!.spec;

  it('labels every day of the range on the time charts', () => {
    expect(panel('metrics.chart.signIns').labels).toHaveLength(3);
    expect(panel('metrics.chart.signIns').series[0].data).toEqual([1, 0, 4]);
  });

  it('splits active users into owners and assistants', () => {
    expect(panel('metrics.chart.activeUsers').series.map((entry) => entry.data)).toEqual([
      [2, 2, 2],
      [1, 1, 1],
    ]);
  });

  it('ranks what was used most, largest first', () => {
    const features = panel('metrics.chart.features');

    expect(features.horizontal).toBe(true);
    expect(features.labels.slice(0, 3)).toEqual([
      'metrics.feature.apartments',
      'metrics.feature.expenses',
      'metrics.feature.buildings',
    ]);
    expect(features.series[0].data.slice(0, 3)).toEqual([6, 3, 1]);
  });

  it('keeps refused and failed requests apart, in their status colours', () => {
    expect(panel('metrics.chart.errors').series.map((entry) => entry.colour)).toEqual([
      'status-warning',
      'status-critical',
    ]);
  });

  it('counts owners by where they stand', () => {
    // Subscribed, expired, on trial, suspended.
    expect(panel('metrics.chart.owners').series[0].data).toEqual([5, 1, 2, 0]);
  });
});

describe('rates', () => {
  it('takes conversions of the trials started, and errors of all requests', () => {
    expect(conversionRate(METRICS)).toBe(0.25);
    expect(errorRate(METRICS)).toBe(0.05);
  });

  it('has no rate before there is anything to take it of', () => {
    const quiet = {
      ...METRICS,
      customers: { ...METRICS.customers, trialsStarted: 0 },
      traffic: { ...METRICS.traffic, requests: 0 },
    };

    expect(conversionRate(quiet)).toBeNull();
    expect(errorRate(quiet)).toBeNull();
  });
});
