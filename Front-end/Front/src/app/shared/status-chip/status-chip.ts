import { Component, computed, input } from '@angular/core';

const ROTULOS: Record<string, string> = {
  PENDENTE: 'Pendente',
  APROVADA: 'Aprovada',
  RECUSADA: 'Recusada',
  PROCESSADA: 'Processada',
  ACEITA: 'Aceita',
  CANCELADA: 'Cancelada',
};

@Component({
  selector: 'app-status-chip',
  standalone: false,
  template: `<span class="chip-status" [class]="classe()">{{ rotulo() }}</span>`,
})
export class StatusChip {
  readonly status = input.required<string>();
  protected readonly rotulo = computed(() => ROTULOS[this.status()] ?? this.status());
  protected readonly classe = computed(() => `chip-status st-${this.status()}`);
}
