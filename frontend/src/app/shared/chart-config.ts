import type { ChartConfiguration } from 'chart.js';

/** The colours a series may take: one of the categorical hues, in order, or a status. */
export type SeriesColour =
  'series-1' | 'series-2' | 'series-3' | 'series-4' | 'status-warning' | 'status-critical';

export interface Series {
  label: string;
  data: number[];
  colour: SeriesColour;
}

/** What a chart shows, independent of Chart.js. */
export interface ChartSpec {
  type: 'line' | 'bar' | 'doughnut';
  labels: string[];
  series: Series[];
  /** Bars laid along the y axis, for ranking categories by size. */
  horizontal?: boolean;
  /** Doughnut slices take one colour each instead of one per series. */
  sliceColours?: SeriesColour[];
}

/** The theme's colours, read off the page: a canvas cannot use var(). */
export interface Palette {
  ink: string;
  ink2: string;
  ink3: string;
  line: string;
  surface: string;
  font: string;
  colours: Record<SeriesColour, string>;
}

const COLOURS: SeriesColour[] = [
  'series-1',
  'series-2',
  'series-3',
  'series-4',
  'status-warning',
  'status-critical',
];

export function readPalette(element: Element): Palette {
  const style = getComputedStyle(element);
  const value = (name: string) => style.getPropertyValue(name).trim();
  return {
    ink: value('--ink'),
    ink2: value('--ink-2'),
    ink3: value('--ink-3'),
    line: value('--line'),
    surface: value('--surface'),
    font: value('--font'),
    colours: Object.fromEntries(COLOURS.map((colour) => [colour, value(`--${colour}`)])) as Record<
      SeriesColour,
      string
    >,
  };
}

/** Builds the Chart.js configuration for a spec, in the theme's colours and the reader's number format. */
export function buildConfig(
  spec: ChartSpec,
  palette: Palette,
  locale: string,
  animate: boolean,
): ChartConfiguration {
  const numbers = new Intl.NumberFormat(locale);
  const doughnut = spec.type === 'doughnut';
  const axis = { grid: { color: palette.line }, border: { color: palette.line } };
  const categories = {
    ...axis,
    ticks: { color: palette.ink3, autoSkip: true, maxTicksLimit: spec.horizontal ? undefined : 8 },
  };
  const values = {
    ...axis,
    beginAtZero: true,
    ticks: {
      color: palette.ink3,
      precision: 0,
      callback: (value: string | number) => numbers.format(Number(value)),
    },
  };
  return {
    type: spec.type,
    data: {
      labels: spec.labels,
      datasets: spec.series.map((series) => {
        const colour = doughnut
          ? (spec.sliceColours ?? []).map((slice) => palette.colours[slice])
          : palette.colours[series.colour];
        return {
          label: series.label,
          data: series.data,
          backgroundColor: colour,
          borderColor: doughnut ? palette.surface : colour,
          borderWidth: 2,
          borderRadius: spec.type === 'bar' ? 4 : 0,
          pointRadius: 0,
          pointHoverRadius: 5,
          tension: 0.25,
        };
      }),
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: animate ? { duration: 400 } : false,
      indexAxis: spec.horizontal ? 'y' : 'x',
      interaction: doughnut
        ? { mode: 'nearest', intersect: true }
        : { mode: 'index', intersect: false },
      font: { family: palette.font },
      plugins: {
        // A single series needs no legend: the panel's title names it.
        legend: {
          display: doughnut || spec.series.length > 1,
          position: 'bottom',
          labels: { color: palette.ink2, usePointStyle: true, font: { family: palette.font } },
        },
        tooltip: {
          backgroundColor: palette.surface,
          titleColor: palette.ink,
          bodyColor: palette.ink2,
          borderColor: palette.line,
          borderWidth: 1,
          titleFont: { family: palette.font },
          bodyFont: { family: palette.font },
          callbacks: {
            label: (item) => {
              const value = spec.horizontal
                ? item.parsed.x
                : doughnut
                  ? item.parsed
                  : item.parsed.y;
              return ` ${item.dataset.label}: ${numbers.format(Number(value))}`;
            },
          },
        },
      },
      scales: doughnut
        ? {}
        : spec.horizontal
          ? { x: values, y: categories }
          : { x: categories, y: values },
    },
  } as ChartConfiguration;
}
