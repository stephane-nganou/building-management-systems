import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterRenderEffect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  DoughnutController,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';

import { TranslationService } from '../core/i18n';
import { ThemeService } from '../core/theme';
import { ChartSpec, buildConfig, readPalette } from './chart-config';

// Only the parts the metrics draw, so the rest of Chart.js stays out of the bundle.
Chart.register(
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  DoughnutController,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
);

/**
 * One Chart.js chart, drawn in the theme's colours and the reader's number
 * format. It is redrawn, not rebuilt, when its data, the theme, the system's
 * light or dark setting, or the language changes.
 *
 * <p>A canvas is a picture to a screen reader, so it carries a label; the
 * values themselves are in the table each panel offers beside it.
 */
@Component({
  selector: 'bms-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="chart-box">
      <canvas #canvas role="img" [attr.aria-label]="label()"></canvas>
    </div>
  `,
})
export class ChartView {
  readonly spec = input.required<ChartSpec>();
  readonly label = input.required<string>();

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private readonly theme = inject(ThemeService);
  private readonly i18n = inject(TranslationService);
  private chart: Chart | null = null;

  /** Classic follows the system's light or dark setting, which no signal reports. */
  private readonly scheme = signal(0);

  constructor() {
    const dark = matchMedia('(prefers-color-scheme: dark)');
    const bump = () => this.scheme.update((count) => count + 1);
    dark.addEventListener('change', bump);
    const animate = !matchMedia('(prefers-reduced-motion: reduce)').matches;

    // After rendering, so the theme's colours are already on the root element.
    afterRenderEffect(() => {
      this.theme.theme();
      this.scheme();
      const config = buildConfig(
        this.spec(),
        readPalette(document.documentElement),
        this.i18n.locale(),
        animate,
      );
      // A panel keeps its kind of chart, so only its data and options change.
      if (this.chart) {
        this.chart.data = config.data;
        this.chart.options = config.options ?? {};
        this.chart.update();
      } else {
        this.chart = new Chart(this.canvas().nativeElement, config);
      }
    });

    inject(DestroyRef).onDestroy(() => {
      dark.removeEventListener('change', bump);
      this.chart?.destroy();
    });
  }
}
