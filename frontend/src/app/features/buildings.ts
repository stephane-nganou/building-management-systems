import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { BuildingsApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { Building } from '../core/models';
import { TranslatePipe } from '../shared/translate.pipe';

interface BuildingForm {
  name: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  notes: string;
}

const blank = (): BuildingForm => ({
  name: '',
  street: '',
  city: '',
  postalCode: '',
  country: '',
  notes: '',
});

@Component({
  selector: 'bms-buildings',
  imports: [FormsModule, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'buildings.title' | t }}</h1>
          <p>{{ 'buildings.subtitle' | t }}</p>
        </div>
        <button class="primary" type="button" (click)="startCreate()">
          {{ 'buildings.add' | t }}
        </button>
      </div>

      @if (buildings.isLoading()) {
        <p class="loading">{{ 'buildings.loading' | t }}</p>
      } @else if (buildings.hasValue() && buildings.value()!.length === 0) {
        <div class="empty">
          <h3>{{ 'buildings.emptyTitle' | t }}</h3>
          <p>{{ 'buildings.emptyBody' | t }}</p>
          <button class="primary" type="button" (click)="startCreate()">
            {{ 'buildings.add' | t }}
          </button>
        </div>
      } @else if (buildings.hasValue()) {
        <table class="sheet">
          <thead>
            <tr>
              <th>{{ 'common.name' | t }}</th>
              <th>{{ 'buildings.address' | t }}</th>
              <th class="right">{{ 'buildings.apartments' | t }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (building of buildings.value(); track building.id) {
              <tr>
                <td class="strong">{{ building.name }}</td>
                <td class="muted">{{ address(building) }}</td>
                <td class="right">{{ building.apartmentCount }}</td>
                <td class="right">
                  <button class="quiet" type="button" (click)="startEdit(building)">
                    {{ 'common.edit' | t }}
                  </button>
                  <button class="quiet danger" type="button" (click)="remove(building)">
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
            <h2>{{ (editingId() ? 'buildings.editTitle' : 'buildings.add') | t }}</h2>
          </header>
          <div class="body">
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
              <label for="notes">{{ 'common.notes' | t }}</label>
              <textarea id="notes" name="notes" rows="3" [(ngModel)]="form.notes"></textarea>
            </div>
          </div>
          <footer>
            <button type="button" (click)="cancel()">{{ 'common.cancel' | t }}</button>
            <button class="primary" type="button" [disabled]="!form.name.trim()" (click)="save()">
              {{ (editingId() ? 'common.saveChanges' : 'buildings.add') | t }}
            </button>
          </footer>
        </div>
      </div>
    }
  `,
})
export class BuildingsPage {
  private api = inject(BuildingsApi);
  private i18n = inject(TranslationService);

  protected readonly buildings = rxResource({ stream: () => this.api.list() });
  protected readonly editing = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected form: BuildingForm = blank();

  protected address(building: Building): string {
    return [building.street, [building.postalCode, building.city].filter(Boolean).join(' '), building.country]
      .filter((part) => part && part.trim())
      .join(', ');
  }

  protected startCreate(): void {
    this.form = blank();
    this.editingId.set(null);
    this.editing.set(true);
  }

  protected startEdit(building: Building): void {
    this.form = {
      name: building.name,
      street: building.street ?? '',
      city: building.city ?? '',
      postalCode: building.postalCode ?? '',
      country: building.country ?? '',
      notes: building.notes ?? '',
    };
    this.editingId.set(building.id);
    this.editing.set(true);
  }

  protected cancel(): void {
    this.editing.set(false);
  }

  protected save(): void {
    const id = this.editingId();
    const request = id ? this.api.update(id, this.form) : this.api.create(this.form);
    request.subscribe({
      next: () => {
        this.editing.set(false);
        this.error.set(null);
        this.buildings.reload();
      },
      error: () => this.error.set(this.i18n.translate('buildings.saveFailed')),
    });
  }

  protected remove(building: Building): void {
    this.api.remove(building.id).subscribe({
      next: () => this.buildings.reload(),
      error: () =>
        this.error.set(this.i18n.translate('buildings.deleteFailed', { name: building.name })),
    });
  }
}
