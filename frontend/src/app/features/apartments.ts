import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { ApartmentsApi, BuildingsApi } from '../core/api';
import { Apartment, ApartmentStatus } from '../core/models';
import { LabelPipe, MoneyPipe } from '../shared/money.pipe';

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
  imports: [FormsModule, MoneyPipe, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>Apartments</h1>
          <p>Units across your buildings, with the rent each one is let for.</p>
        </div>
        <button class="primary" type="button" [disabled]="!hasBuildings()" (click)="startCreate()">
          Add apartment
        </button>
      </div>

      <div class="toolbar">
        <div class="field">
          <label for="filter">Building</label>
          <select id="filter" [ngModel]="buildingFilter()" (ngModelChange)="buildingFilter.set($event)">
            <option value="">All buildings</option>
            @for (building of buildings.value(); track building.id) {
              <option [value]="building.id">{{ building.name }}</option>
            }
          </select>
        </div>
      </div>

      @if (apartments.isLoading()) {
        <p class="loading">Loading apartments.</p>
      } @else if (!hasBuildings()) {
        <div class="empty">
          <h3>Add a building first</h3>
          <p>Apartments belong to a building, so create one before adding units.</p>
        </div>
      } @else if (apartments.hasValue() && apartments.value()!.length === 0) {
        <div class="empty">
          <h3>No apartments here</h3>
          <p>Add the units in this building, with their rent and room layout.</p>
          <button class="primary" type="button" (click)="startCreate()">Add apartment</button>
        </div>
      } @else if (apartments.hasValue()) {
        <table class="sheet">
          <thead>
            <tr>
              <th>Unit</th>
              <th>Building</th>
              <th class="right">Floor</th>
              <th class="right">Size</th>
              <th class="right">Rooms</th>
              <th class="right">Rent</th>
              <th class="right">Utilities</th>
              <th>Status</th>
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
                    {{ apartment.status | label }}
                  </span>
                </td>
                <td class="right">
                  <button class="quiet" type="button" (click)="startEdit(apartment)">Edit</button>
                  <button class="quiet danger" type="button" (click)="remove(apartment)">Delete</button>
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
            <h2>{{ editingId() ? 'Edit apartment' : 'Add apartment' }}</h2>
          </header>
          <div class="body">
            @if (!editingId()) {
              <div class="field">
                <label for="building">Building</label>
                <select id="building" [(ngModel)]="targetBuildingId">
                  @for (building of buildings.value(); track building.id) {
                    <option [value]="building.id">{{ building.name }}</option>
                  }
                </select>
              </div>
            }
            <div class="grid-2">
              <div class="field">
                <label for="label">Number or name</label>
                <input id="label" [(ngModel)]="form.label" placeholder="1A" />
              </div>
              <div class="field">
                <label for="status">Status</label>
                <select id="status" [(ngModel)]="form.status">
                  <option value="VACANT">Vacant</option>
                  <option value="OCCUPIED">Occupied</option>
                  <option value="MAINTENANCE">Maintenance</option>
                </select>
              </div>
              <div class="field">
                <label for="floor">Floor</label>
                <input id="floor" type="number" [(ngModel)]="form.floor" />
              </div>
              <div class="field">
                <label for="sizeSqm">Size in m2</label>
                <input id="sizeSqm" type="number" step="0.01" [(ngModel)]="form.sizeSqm" />
              </div>
              <div class="field">
                <label for="baseRent">Monthly rent</label>
                <input id="baseRent" type="number" step="0.01" [(ngModel)]="form.baseRent" />
              </div>
              <div class="field">
                <label for="utilitiesAdvance">Utilities advance</label>
                <input
                  id="utilitiesAdvance"
                  type="number"
                  step="0.01"
                  [(ngModel)]="form.utilitiesAdvance"
                />
              </div>
              <div class="field">
                <label for="rooms">Rooms</label>
                <input id="rooms" type="number" [(ngModel)]="form.rooms" />
              </div>
              <div class="field">
                <label for="bedrooms">Bedrooms</label>
                <input id="bedrooms" type="number" [(ngModel)]="form.bedrooms" />
              </div>
              <div class="field">
                <label for="bathrooms">Bathrooms</label>
                <input id="bathrooms" type="number" [(ngModel)]="form.bathrooms" />
              </div>
              <div class="field">
                <label for="kitchens">Kitchens</label>
                <input id="kitchens" type="number" [(ngModel)]="form.kitchens" />
              </div>
              <div class="field">
                <label for="toilets">Toilets</label>
                <input id="toilets" type="number" [(ngModel)]="form.toilets" />
              </div>
            </div>
          </div>
          <footer>
            <button type="button" (click)="cancel()">Cancel</button>
            <button class="primary" type="button" [disabled]="!form.label.trim()" (click)="save()">
              {{ editingId() ? 'Save changes' : 'Add apartment' }}
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
        this.error.set(response?.error?.detail ?? 'The apartment could not be saved.'),
    });
  }

  protected remove(apartment: Apartment): void {
    this.api.remove(apartment.id).subscribe({
      next: () => {
        this.apartments.reload();
        this.buildings.reload();
      },
      error: () => this.error.set(`Apartment ${apartment.label} could not be deleted.`),
    });
  }
}
