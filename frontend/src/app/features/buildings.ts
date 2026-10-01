import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { ApartmentsApi, BuildingsApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { Building, CURRENCIES, Currency } from '../core/models';
import { SessionService } from '../core/session';
import { ConfirmService } from '../shared/confirm';
import { Dialog } from '../shared/dialog';
import { Facade, Unit } from '../shared/facade';
import { Icon } from '../shared/icon';
import { IconButton } from '../shared/icon-button';
import { ToastService } from '../shared/toasts';
import { TranslatePipe } from '../shared/translate.pipe';

interface BuildingForm {
  name: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  notes: string;
  currency: Currency;
}

const blank = (): BuildingForm => ({
  name: '',
  street: '',
  city: '',
  postalCode: '',
  country: '',
  notes: '',
  currency: 'EUR',
});

@Component({
  selector: 'bms-buildings',
  imports: [FormsModule, Dialog, Facade, Icon, IconButton, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'buildings.title' | t }}</h1>
          <p>{{ 'buildings.subtitle' | t }}</p>
        </div>
        <button class="primary" type="button" (click)="startCreate()">
          <bms-icon name="plus" />
          {{ 'buildings.add' | t }}
        </button>
      </div>

      @if (buildings.isLoading()) {
        <p class="loading">{{ 'buildings.loading' | t }}</p>
      } @else if (buildings.hasValue() && buildings.value()!.length === 0) {
        <div class="empty">
          <bms-facade [units]="[]" [scale]="2" />
          <div>
            <h3>{{ 'buildings.emptyTitle' | t }}</h3>
            <p>{{ 'buildings.emptyBody' | t }}</p>
            <button class="primary" type="button" (click)="startCreate()">
              <bms-icon name="plus" />
              {{ 'buildings.add' | t }}
            </button>
          </div>
        </div>
      } @else if (buildings.hasValue()) {
        <div class="sheet-frame">
          <table class="sheet">
            <thead>
              <tr>
                <th>{{ 'common.name' | t }}</th>
                <th>{{ 'buildings.address' | t }}</th>
                <th class="right">{{ 'buildings.apartments' | t }}</th>
                <th><span class="visually-hidden">{{ 'common.actions' | t }}</span></th>
              </tr>
            </thead>
            <tbody>
              @for (building of buildings.value(); track building.id) {
                <tr>
                  <td>
                    <span class="name-cell">
                      @if (seesApartments) {
                        <bms-facade [units]="unitsOf(building.id)" [scale]="0.8" />
                      }
                      <strong>{{ building.name }}</strong>
                    </span>
                  </td>
                  <td class="muted">{{ address(building) }}</td>
                  <td class="right">{{ building.apartmentCount }}</td>
                  <td class="actions-cell">
                    <span class="row-actions">
                      <button bmsIconButton icon="edit" [label]="'common.edit' | t" (click)="startEdit(building)"></button>
                      <button bmsIconButton class="danger" icon="trash" [label]="'common.delete' | t" (click)="remove(building)"></button>
                    </span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      @if (error()) {
        <p class="notice" role="alert"><bms-icon name="alert" />{{ error() }}</p>
      }
    </section>

    @if (editing()) {
      <bms-dialog [heading]="(editingId() ? 'buildings.editTitle' : 'buildings.add') | t" [error]="error()" (closed)="cancel()">
        <div class="field">
          <label for="name">{{ 'common.name' | t }}</label>
          <input id="name" name="name" [(ngModel)]="form.name" placeholder="Hauptstrasse 1" />
        </div>
        <div class="grid-2">
          <div class="field">
            <label for="street">{{ 'buildings.street' | t }}</label>
            <input id="street" name="street" [(ngModel)]="form.street" />
          </div>
          <div class="field">
            <label for="postalCode">{{ 'buildings.postalCode' | t }}</label>
            <input id="postalCode" name="postalCode" [(ngModel)]="form.postalCode" />
          </div>
          <div class="field">
            <label for="city">{{ 'buildings.city' | t }}</label>
            <input id="city" name="city" [(ngModel)]="form.city" />
          </div>
          <div class="field">
            <label for="country">{{ 'buildings.country' | t }}</label>
            <input id="country" name="country" [(ngModel)]="form.country" />
          </div>
        </div>
        <div class="field">
          <label for="currency">{{ 'buildings.currency' | t }}</label>
          <select id="currency" name="currency" [(ngModel)]="form.currency">
            @for (code of currencies; track code) {
              <option [value]="code">{{ currencyName(code) }} ({{ code }})</option>
            }
          </select>
          @if (editingId()) {
            <p class="hint">{{ 'buildings.currencyHint' | t }}</p>
          }
        </div>
        <div class="field">
          <label for="notes">{{ 'common.notes' | t }}</label>
          <textarea id="notes" name="notes" rows="3" [(ngModel)]="form.notes"></textarea>
        </div>
        <ng-container actions>
          <button type="button" (click)="cancel()">{{ 'common.cancel' | t }}</button>
          <button class="primary" type="button" [disabled]="!form.name.trim()" (click)="save()">
            {{ (editingId() ? 'common.saveChanges' : 'buildings.add') | t }}
          </button>
        </ng-container>
      </bms-dialog>
    }
  `,
})
export class BuildingsPage {
  private api = inject(BuildingsApi);
  private apartmentsApi = inject(ApartmentsApi);
  private i18n = inject(TranslationService);
  private confirm = inject(ConfirmService);
  private toasts = inject(ToastService);

  /** The facades need the apartments, which an assistant may not be allowed to read. */
  protected readonly seesApartments = inject(SessionService).can('APARTMENT_READ');

  protected readonly buildings = rxResource({ stream: () => this.api.list() });
  protected readonly apartments = rxResource({
    params: () => (this.seesApartments ? {} : undefined),
    stream: () => this.apartmentsApi.list(),
    defaultValue: [],
  });

  protected readonly editing = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected form: BuildingForm = blank();
  protected readonly currencies = CURRENCIES;

  private readonly unitsByBuilding = computed(() => {
    const units = new Map<string, Unit[]>();
    for (const apartment of this.apartments.value()) {
      units.set(apartment.buildingId, [...(units.get(apartment.buildingId) ?? []), apartment]);
    }
    return units;
  });

  protected unitsOf(buildingId: string): Unit[] {
    return this.unitsByBuilding().get(buildingId) ?? [];
  }

  protected address(building: Building): string {
    return [building.street, [building.postalCode, building.city].filter(Boolean).join(' '), building.country]
      .filter((part) => part && part.trim())
      .join(', ');
  }

  protected startCreate(): void {
    this.error.set(null);
    this.form = blank();
    this.editingId.set(null);
    this.editing.set(true);
  }

  protected startEdit(building: Building): void {
    this.error.set(null);
    this.form = {
      name: building.name,
      street: building.street ?? '',
      city: building.city ?? '',
      postalCode: building.postalCode ?? '',
      country: building.country ?? '',
      notes: building.notes ?? '',
      currency: building.currency,
    };
    this.editingId.set(building.id);
    this.editing.set(true);
  }

  protected cancel(): void {
    this.editing.set(false);
  }

  /** The currency's name in the app's language, such as "franc CFA (BEAC)" in French. */
  protected currencyName(code: Currency): string {
    return new Intl.DisplayNames([this.i18n.locale()], { type: 'currency' }).of(code) ?? code;
  }

  protected save(): void {
    const id = this.editingId();
    const request = id ? this.api.update(id, this.form) : this.api.create(this.form);
    request.subscribe({
      next: (saved) => {
        this.editing.set(false);
        this.error.set(null);
        this.toasts.show(this.i18n.translate('toast.saved', { name: saved.name }));
        this.buildings.reload();
      },
      error: (response) =>
        this.error.set(response?.error?.detail ?? this.i18n.translate('buildings.saveFailed')),
    });
  }

  protected async remove(building: Building): Promise<void> {
    if (!(await this.confirm.delete(building.name))) {
      return;
    }
    this.api.remove(building.id).subscribe({
      next: () => {
        this.toasts.show(this.i18n.translate('toast.deleted', { name: building.name }));
        this.buildings.reload();
      },
      error: (response) =>
        this.error.set(
          response?.error?.detail ?? this.i18n.translate('buildings.deleteFailed', { name: building.name }),
        ),
    });
  }
}
