import { DayCount, Metrics } from '../core/models';
import { MessageKey } from '../i18n/en';
import { ChartSpec } from '../shared/chart-config';

type Translate = (key: MessageKey) => string;

/** The panels the metrics screen draws, each with its title. */
export interface Panel {
  title: MessageKey;
  spec: ChartSpec;
}

/** A count as the reader writes it; averages are rounded to the whole number. */
export function count(value: number, locale: string): string {
  return new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(value);
}

/** A UTC day from the API, as the reader writes a day and month. */
export function dayLabels(days: string[], locale: string): string[] {
  const format = new Intl.DateTimeFormat(locale, {
    day: 'numeric',
    month: 'short',
    timeZone: 'UTC',
  });
  return days.map((day) => format.format(new Date(`${day}T00:00:00Z`)));
}

function counts(series: DayCount[]): number[] {
  return series.map((entry) => entry.count);
}

export function total(series: DayCount[]): number {
  return series.reduce((sum, entry) => sum + entry.count, 0);
}

/** The "most used" ranking: what was created or downloaded over the range, largest first. */
function features(metrics: Metrics, t: Translate): ChartSpec {
  const f = metrics.features;
  const ranked = (
    [
      ['metrics.feature.buildings', f.buildings],
      ['metrics.feature.apartments', f.apartments],
      ['metrics.feature.tenants', f.tenants],
      ['metrics.feature.expenses', f.expenses],
      ['metrics.feature.rentInvoices', f.rentInvoices],
      ['metrics.feature.coldWaterInvoices', f.coldWaterInvoices],
      ['metrics.feature.pdfDownloads', f.pdfDownloads],
    ] as [MessageKey, DayCount[]][]
  )
    .map(([key, series]) => ({ label: t(key), count: total(series) }))
    .sort((a, b) => b.count - a.count);
  return {
    type: 'bar',
    horizontal: true,
    labels: ranked.map((row) => row.label),
    series: [
      {
        label: t('metrics.series.count'),
        data: ranked.map((row) => row.count),
        colour: 'series-1',
      },
    ],
  };
}

export function panels(metrics: Metrics, t: Translate, locale: string): Panel[] {
  const labels = dayLabels(
    metrics.activity.signIns.map((entry) => entry.day),
    locale,
  );
  const customers = metrics.customers;
  return [
    {
      title: 'metrics.chart.signIns',
      spec: {
        type: 'line',
        labels,
        series: [
          {
            label: t('metrics.series.signIns'),
            data: counts(metrics.activity.signIns),
            colour: 'series-1',
          },
        ],
      },
    },
    {
      title: 'metrics.chart.activeUsers',
      spec: {
        type: 'line',
        labels,
        series: [
          {
            label: t('metrics.series.owners'),
            data: metrics.activity.activeUsers.map((day) => day.owners),
            colour: 'series-1',
          },
          {
            label: t('metrics.series.assistants'),
            data: metrics.activity.activeUsers.map((day) => day.assistants),
            colour: 'series-2',
          },
        ],
      },
    },
    {
      // The slices follow the palette's validated order, so the statuses are
      // ordered to fall on fitting hues: nothing that has ended reads green.
      title: 'metrics.chart.owners',
      spec: {
        type: 'doughnut',
        labels: [
          t('metrics.status.active'),
          t('metrics.status.expired'),
          t('metrics.status.trial'),
          t('metrics.status.suspended'),
        ],
        series: [
          {
            label: t('metrics.series.owners'),
            data: [customers.active, customers.expired, customers.trial, customers.suspended],
            colour: 'series-1',
          },
        ],
        sliceColours: ['series-1', 'series-2', 'series-3', 'series-4'],
      },
    },
    {
      title: 'metrics.chart.newOwners',
      spec: {
        type: 'line',
        labels,
        series: [
          {
            label: t('metrics.series.newOwners'),
            data: counts(customers.newOwners),
            colour: 'series-1',
          },
        ],
      },
    },
    { title: 'metrics.chart.features', spec: features(metrics, t) },
    {
      title: 'metrics.chart.requests',
      spec: {
        type: 'line',
        labels,
        series: [
          {
            label: t('metrics.series.requests'),
            data: metrics.traffic.perDay.map((day) => day.requests),
            colour: 'series-1',
          },
        ],
      },
    },
    {
      title: 'metrics.chart.errors',
      spec: {
        type: 'line',
        labels,
        series: [
          {
            label: t('metrics.series.clientErrors'),
            data: metrics.traffic.perDay.map((day) => day.clientErrors),
            colour: 'status-warning',
          },
          {
            label: t('metrics.series.serverErrors'),
            data: metrics.traffic.perDay.map((day) => day.serverErrors),
            colour: 'status-critical',
          },
        ],
      },
    },
  ];
}

/** Trials that became a subscription, of those started in the range; null before there is one. */
export function conversionRate(metrics: Metrics): number | null {
  const started = metrics.customers.trialsStarted;
  return started === 0 ? null : metrics.customers.trialsConverted / started;
}

export function errorRate(metrics: Metrics): number | null {
  const traffic = metrics.traffic;
  return traffic.requests === 0
    ? null
    : (traffic.clientErrors + traffic.serverErrors) / traffic.requests;
}
