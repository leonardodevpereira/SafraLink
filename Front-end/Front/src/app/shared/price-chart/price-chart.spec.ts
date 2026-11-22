import { TestBed } from '@angular/core/testing';
import { PriceChart } from './price-chart';

describe('PriceChart', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ declarations: [PriceChart] }).compileComponents();
  });

  it('desenha a linha e um ponto por preço', () => {
    const fixture = TestBed.createComponent(PriceChart);
    fixture.componentRef.setInput('pontos', [
      { data: '2026-11-01T08:00:00Z', valor: 120 },
      { data: '2026-11-02T08:00:00Z', valor: 125.5 },
      { data: '2026-11-03T08:00:00Z', valor: 123 },
    ]);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('svg')).not.toBeNull();
    expect(el.querySelector('path.linha')?.getAttribute('d')).toMatch(/^M/);
    expect(el.querySelectorAll('circle.ponto').length).toBe(3);
  });

  it('mostra um aviso quando não há preços', () => {
    const fixture = TestBed.createComponent(PriceChart);
    fixture.componentRef.setInput('pontos', []);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('svg')).toBeNull();
    expect(el.textContent).toContain('Ainda não há preços');
  });

  it('não quebra com um único ponto ou preços iguais', () => {
    const fixture = TestBed.createComponent(PriceChart);
    fixture.componentRef.setInput('pontos', [{ data: '2026-11-01T08:00:00Z', valor: 100 }]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('circle.ponto').length).toBe(1);
  });
});
