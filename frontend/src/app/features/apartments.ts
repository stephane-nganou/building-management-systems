import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { ApartmentsApi, BuildingsApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { Apartment, ApartmentStatus } from '../core/models';
import { LabelPipe, MoneyPipe } from '../shared/money.pipe';
import { TranslatePipe } from '../shared/translate.pipe';

const STATUSES: ApartmentStatus[] = ['VACANT', 'OCCUPIED', 'MAINTENANCE'];

interface ApartmentForm {
  label: string;
  floor: number | null;
  sizeSqm: number | null;
  rooms: number;
  bedrooms: number;
  bathrooms: number;
  kitchens: number;
  toilets: number;
  baseRent: number;
  utilitiesAdvance: number;
  status: ApartmentStatus;
}

const blank = (): ApartmentForm => ({
  label: '',
  floor: null,
  sizeSqm: null,
  rooms: 1,
  bedrooms: 1,
  bathrooms: 1,
  kitchens: 1,
  toilets: 1,
  baseRent: 0,
  utilitiesAdvance: 0,
  status: 'VACANT',
});

@Component({
  selector: 'bms-apartments',
  imports: [FormsModule, MoneyPipe, LabelPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'apartments.title' | t }}</h1>
          <p>{{ 'apartments.subtitle' | t }}</p>
        </div>
        <button class="primary" type="button" [disabled]="!hasBuildings()" (click)="startCreate()">
          {{ 'apartments.add' | t }}
        </button>
      </div>

      <div class="toolbar">
        <div class="field">
          <label for="filter">{{ 'common.building' | t }}</label>
          <select id="filter" [ngModel]="buildingFilter()" (ngModelChange)="buildingFilter.set($event)">
            <option value="">{{ 'common.allBuildings' | t }}</option>
            @for (building of buildings.value(); track building.id) {
              <option [value]="building.id">{{ building.name }}</option>
            }
          </select>
        </div>
      </div>

      @if (apartments.isLoading()) {
        <p class="loading">{{ 'apartments.loading' | t }}</p>
      } @else if (!hasBuildings()) {
        <div class="empty">
          <h3>{{ 'apartments.needBuildingTitle' | t }}</h3>
          <p>{{ 'apartments.needBuildingBody' | t }}</p>
        </div>
      } @else if (apartments.hasValue() && apartments.value()!.length === 0) {
        <div class="empty">
          <h3>{{ 'apartments.emptyTitle' | t }}</h3>
          <p>{{ 'apartments.emptyBody' | t }}</p>
          <button class="primary" type="button" (click)="startCreate()">
            {{ 'apartments.add' | t }}
          </button>
        </div>
      } @else if (apartments.hasValue()) {
        <table class="sheet">
          <thead>
            <tr>
              <th>{{ 'apartments.unit' | t }}</th>
              <th>{{ 'common.building' | t }}</th>
              <th class="right">{{ 'apartments.floor' | t }}</th>
              <th class="right">{{ 'apartments.size' | t }}</th>
              <th class="right">{{ 'apartments.rooms' | t }}</th>
              <th class="right">{{ 'apartments.rent' | t }}</th>
              <th class="right">{{ 'apartments.utilities' | t }}</th>
              <th>{{ 'common.status' | t }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (apartment of apartments.value(); track apartment.id) {
              <tr>
                <td class="strong">{{ apartment.label }}</td>
                <td class="muted">{{ apartment.buildingName }}</td>
                <td class="right">{{ apartment.floor ?? '-' }}</td>
                <td class="right">{{ apartment.sizeSqm ? apartment.sizeSqm + ' m2' : '-' }}</td>
                <td class="right">{{ apartment.rooms }}</td>
                <td class="right">{{ apartment.baseRent | money }}</td>
                <td class="right muted">{{ apartment.utilitiesAdvance | money }}</td>
                <td>
                  <span class="mark {{ apartment.status.toLowerCase() }}">
                    {{ apartment.status | label: 'status' }}
                  </span>
                </td>
                <td class="right">
                  <button class="quiet" type="button" (click)="startEdit(apartment)">
                    {{ 'common.edit' | t }}
                  </button>
                  <button class="quiet danger" type="button" (click)="remove(apartment)">
                    {{ 'common.delete' | t }}
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }

      @if (error()) {
        <p class="notice">{{ error() }}</p>
      }
    </section>

    @if (editing()) {
      <div class="scrim" (click)="cancel()">
        <div class="panel" (click)="$event.stopPropagation()">
          <header>
            <h2>{{ (editingId() ? 'apartments.editTitle' : 'apartments.add') | t }}</h2>
          </header>
          <div class="body">
            @if (!editingId()) {
              <div class="field">
                <label for="building">{{ 'common.building' | t }}</label>
                <select id="building" [(ngModel)]="targetBuildingId">
                  @for (building of buildings.value(); track building.id) {
                    <option [value]="building.id">{{ building.name }}</option>
                  }
                </select>
              </div>
            }
            <div class="grid-2">
              <div class="field">
                <label for="label">{{ 'apartments.label' | t }}</label>
                <input id="label" [(ngModel)]="form.label" placeholder="1A" />
              </div>
              <div class="field">
                <label for="status">{{ 'common.status' | t }}</label>
                <select id="status" [(ngModel)]="form.status">
                  @for (status of statuses; track status) {
                    <option [value]="status">{{ status | label: 'status' }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label for="floor">{{ 'apartments.floor' | t }}</label>
                <input id="floor" type="number" [(ngModel)]="form.floor" />
              </div>
              <div class="field">
                <label for="sizeSqm">{{ 'apartments.sizeSqm' | t }}</label>
                <input id="sizeSqm" type="number" step="0.01" [(ngModel)]="form.sizeSqm" />
              </div>
              <div class="field">
                <label for="baseRent">{{ 'apartments.monthlyRent' | t }}</label>
                <input id="baseRent" type="number" step="0.01" [(ngModel)]="form.baseRent" />
              </div>
              <div class="field">
                <label for="utilitiesAdvance">{{ 'apartments.utilitiesAdvance' | t }}</label>
                <input
                  id="utilitiesAdvance"
                  type="number"
                  step="0.01"
                  [(ngModel)]="form.utilitiesAdvance"
                />
              </div>
              <div class="field">
                <label for="rooms">{{ 'apartments.rooms' | t }}</label>
                <input id="rooms" type="number" [(ngModel)]="form.rooms" />
              </div>
              <div class="field">
                <label for="bedrooms">{{ 'apartments.bedrooms' | t }}</label>
                <input id="bedrooms" type="number" [(ngModel)]="form.bedrooms" />
              </div>
              <div class="field">
                <label for="bathrooms">{{ 'apartments.bathrooms' | t }}</label>
                <input id="bathrooms" type="number" [(ngModel)]="form.bathrooms" />
              </div>
              <div class="field">
                <label for="kitchens">{{ 'apartments.kitchens' | t }}</label>
                <input id="kitchens" type="number" [(ngModel)]="form.kitchens" />
              </div>
              <div class="field">
                <label for="toilets">{{ 'apartments.toilets' | t }}</label>
                <input id="toilets" type="number" [(ngModel)]="form.toilets" />
              </div>
            </div>
          </div>
          <footer>
            <button type="button" (click)="cancel()">{{ 'common.cancel' | t }}</button>
            <button class="primary" type="button" [disabled]="!form.label.trim()" (click)="save()">
              {{ (editingId() ? 'common.saveChanges' : 'apartments.add') | t }}
            </button>
          </footer>
        </div>
      </div>
    }
  `,
})
export class ApartmentsPage {
  private api = inject(ApartmentsApi);
  private buildingsApi = inject(BuildingsApi);
  private i18n = inject(TranslationService);

  protected readonly statuses = STATUSES;
  protected readonly buildingFilter = signal('');
  protected readonly editing = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected form: ApartmentForm = blank();
  protected targetBuildingId = '';

  protected readonly buildings = rxResource({
    stream: () => this.buildingsApi.list(),
    defaultValue: [],
  });

  protected readonly apartments = rxResource({
    params: () => ({ buildingId: this.buildingFilter() }),
    stream: ({ params }) => this.api.list(params.buildingId || undefined),
  });

  protected hasBuildings(): boolean {
    return this.buildings.value().length > 0;
  }

  protected startCreate(): void {
    this.form = blank();
    this.editingId.set(null);
    this.targetBuildingId = this.buildingFilter() || this.buildings.value()[0]?.id || '';
    this.editing.set(true);
  }

  protected startEdit(apartment: Apartment): void {
    this.form = {
      label: apartment.label,
      floor: apartment.floor,
      sizeSqm: apartment.sizeSqm,
      rooms: apartment.rooms,
      bedrooms: apartment.bedrooms,
      bathrooms: apartment.bathrooms,
      kitchens: apartment.kitchens,
      toilets: apartment.toilets,
      baseRent: apartment.baseRent,
      utilitiesAdvance: apartment.utilitiesAdvance,
      status: apartment.status,
    };
    this.editingId.set(apartment.id);
    this.editing.set(true);
  }

  protected cancel(): void {
    this.editing.set(false);
  }

  protected save(): void {
    const id = this.editingId();
    const request = id
      ? this.api.update(id, this.form)
      : this.api.create(this.targetBuildingId, this.form);
    request.subscribe({
      next: () => {
        this.editing.set(false);
        this.error.set(null);
        this.apartments.reload();
        this.buildings.reload();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('apartments.saveFailed')),
    });
  }

  protected remove(apartment: Apartment): void {
    this.api.remove(apartment.id).subscribe({
      next: () => {
        this.apartments.reload();
        this.buildings.reload();
      },
      error: () =>
        this.error.set(this.i18n.translate('apartments.deleteFailed', { label: apartment.label })),
    });
  }
}
