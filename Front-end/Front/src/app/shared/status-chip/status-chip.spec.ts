import { TestBed } from '@angular/core/testing';
import { StatusChip } from './status-chip';

describe('StatusChip', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ declarations: [StatusChip] }).compileComponents();
  });

  it('mostra o rótulo em português e a classe da situação', () => {
    const fixture = TestBed.createComponent(StatusChip);
    fixture.componentRef.setInput('status', 'PROCESSADA');
    fixture.detectChanges();
    const chip = (fixture.nativeElement as HTMLElement).querySelector('.chip-status');
    expect(chip?.textContent?.trim()).toBe('Processada');
    expect(chip?.classList.contains('st-PROCESSADA')).toBe(true);
  });
});
