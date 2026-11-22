import { TestBed } from '@angular/core/testing';
import { GraoIcon } from './grao-icon';

describe('GraoIcon', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ declarations: [GraoIcon] }).compileComponents();
  });

  for (const grao of ['SOJA', 'MILHO', 'FEIJAO'] as const) {
    it(`desenha o selo de ${grao}`, () => {
      const fixture = TestBed.createComponent(GraoIcon);
      fixture.componentRef.setInput('grao', grao);
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('svg')).not.toBeNull();
      expect(el.querySelector('.selo')?.classList.contains(grao.toLowerCase())).toBe(true);
    });
  }
});
