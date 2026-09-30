import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { Facade, floorsOf } from './facade';

describe('floorsOf', () => {
  it('stacks units into floors, top floor first', () => {
    expect(
      floorsOf([
        { floor: 0, status: 'VACANT' },
        { floor: 2, status: 'OCCUPIED' },
        { floor: 0, status: 'OCCUPIED' },
        { floor: 1, status: 'MAINTENANCE' },
      ]),
    ).toEqual([['OCCUPIED'], ['MAINTENANCE'], ['VACANT', 'OCCUPIED']]);
  });

  it('puts a unit without a floor on the ground floor', () => {
    expect(
      floorsOf([
        { floor: null, status: 'OCCUPIED' },
        { floor: 0, status: 'VACANT' },
        { floor: 1, status: 'VACANT' },
      ]),
    ).toEqual([['VACANT'], ['OCCUPIED', 'VACANT']]);
  });

  it('draws no floors for a building without apartments', () => {
    expect(floorsOf([])).toEqual([]);
  });
});

describe('Facade', () => {
  beforeEach(() => localStorage.setItem('bms.language', 'en'));

  it('draws one window per apartment, lit only where it is let', () => {
    const fixture = TestBed.createComponent(Facade);
    fixture.componentRef.setInput('units', [
      { floor: 0, status: 'OCCUPIED' },
      { floor: 0, status: 'VACANT' },
      { floor: 1, status: 'OCCUPIED' },
    ]);
    fixture.detectChanges();

    const svg: SVGElement = fixture.nativeElement.querySelector('svg');
    expect(svg.querySelectorAll('.pane')).toHaveLength(3);
    expect(svg.querySelectorAll('.pane.occupied')).toHaveLength(2);
    expect(svg.getAttribute('aria-label')).toBe('2 of 3 apartments let');
  });
});
