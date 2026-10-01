import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../core/auth';
import { Brand } from '../shared/brand';
import { Facade } from '../shared/facade';
import { Icon } from '../shared/icon';
import { LanguageSwitcher } from '../shared/language-switcher';
import { ThemeSwitcher } from '../shared/theme-switcher';
import { TranslatePipe } from '../shared/translate.pipe';
import { CONTACTS, FEATURES, QUESTIONS, STEPS, STREET } from './home-content';

/** What a visitor who is not signed in sees: what Hausbuch does, and a free month to try it. */
@Component({
  selector: 'bms-home',
  imports: [Brand, Facade, Icon, LanguageSwitcher, RouterLink, ThemeSwitcher, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="home-skip" href="#main">{{ 'home.skip' | t }}</a>

    <header class="home-hero night">
      <div class="home-wrap home-top">
        <bms-brand />
        <nav class="home-top-actions" [attr.aria-label]="'home.nav' | t">
          <button class="home-sign-in" type="button" (click)="signIn()">
            {{ 'home.signIn' | t }}
          </button>
          <a class="btn home-lit" routerLink="/register">{{ 'home.trial.cta' | t }}</a>
        </nav>
      </div>

      <div class="home-wrap home-hero-copy">
        <h1>{{ 'home.hero.title' | t }}</h1>
        <p class="home-lead">{{ 'home.hero.lead' | t }}</p>
        <div class="home-cta-row">
          <a class="btn home-lit" routerLink="/register">{{ 'home.trial.cta' | t }}</a>
          <a class="btn home-ghost" href="#contact">{{ 'home.hero.talk' | t }}</a>
        </div>
        <p class="home-reassure">{{ 'home.trial.note' | t }}</p>
      </div>

      <div class="home-street" aria-hidden="true">
        @for (units of street; track $index) {
          <bms-facade [units]="units" [scale]="2" [animate]="true" />
        }
      </div>
    </header>

    <main id="main">
      <section class="home-section" aria-labelledby="features-title">
        <div class="home-wrap">
          <h2 id="features-title">{{ 'home.features.title' | t }}</h2>
          <p class="home-section-lead">{{ 'home.features.lead' | t }}</p>
          <ul class="home-features">
            @for (feature of features; track feature.title) {
              <li>
                <bms-icon [name]="feature.icon" [size]="22" />
                <h3>{{ feature.title | t }}</h3>
                <p>{{ feature.body | t }}</p>
              </li>
            }
          </ul>
        </div>
      </section>

      <section class="home-section home-sunk" aria-labelledby="how-title">
        <div class="home-wrap">
          <h2 id="how-title">{{ 'home.how.title' | t }}</h2>
          <ol class="home-steps">
            @for (step of steps; track step.title) {
              <li>
                <h3>{{ step.title | t }}</h3>
                <p>{{ step.body | t }}</p>
              </li>
            }
          </ol>
        </div>
      </section>

      <section class="home-trial night" aria-labelledby="trial-title">
        <div class="home-wrap home-trial-inner">
          <div>
            <h2 id="trial-title">{{ 'home.trial.title' | t }}</h2>
            <p>{{ 'home.trial.body' | t }}</p>
          </div>
          <a class="btn home-lit" routerLink="/register">{{ 'home.trial.cta' | t }}</a>
        </div>
      </section>

      <section class="home-section" aria-labelledby="faq-title">
        <div class="home-wrap home-narrow">
          <h2 id="faq-title">{{ 'home.faq.title' | t }}</h2>
          <div class="home-faq">
            @for (item of questions; track item.question) {
              <details>
                <summary>{{ item.question | t }}</summary>
                <p>{{ item.answer | t }}</p>
              </details>
            }
          </div>
        </div>
      </section>

      <section id="contact" class="home-section home-sunk" aria-labelledby="contact-title">
        <div class="home-wrap home-narrow">
          <h2 id="contact-title">{{ 'home.contact.title' | t }}</h2>
          <p class="home-section-lead">{{ 'home.contact.lead' | t }}</p>
          <ul class="home-contacts">
            @for (contact of contacts; track contact.value) {
              <li>
                <bms-icon [name]="contact.icon" [size]="20" />
                <span class="home-contact-label">{{ contact.label | t }}</span>
                @if (contact.href) {
                  <a [href]="contact.href">{{ contact.value }}</a>
                } @else {
                  <span>{{ contact.value }}</span>
                }
              </li>
            }
          </ul>
        </div>
      </section>
    </main>

    <footer class="home-foot">
      <div class="home-wrap home-foot-inner">
        <p>{{ 'home.foot.rights' | t: { year: year } }}</p>
        <div class="home-settings">
          <bms-language-switcher />
          <bms-theme-switcher />
        </div>
      </div>
    </footer>
  `,
})
export class HomePage {
  private auth = inject(AuthService);

  protected readonly street = STREET;
  protected readonly features = FEATURES;
  protected readonly steps = STEPS;
  protected readonly questions = QUESTIONS;
  protected readonly contacts = CONTACTS;
  protected readonly year = new Date().getFullYear();

  protected signIn(): void {
    this.auth.signIn('/');
  }
}
