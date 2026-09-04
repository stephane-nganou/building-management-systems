import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { SessionService } from '../core/session';

/**
 * The empty path. A plain redirect cannot be used here, because the router
 * resolves redirects before guards run and the first page a user may see is
 * only known once their permissions are loaded.
 */
@Component({
  selector: 'bms-landing',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '',
})
export class LandingPage {
  constructor() {
    const router = inject(Router);
    void router.navigateByUrl(inject(SessionService).landingRoute(), { replaceUrl: true });
  }
}
