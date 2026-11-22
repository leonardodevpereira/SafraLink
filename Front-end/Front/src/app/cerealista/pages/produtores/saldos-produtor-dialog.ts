import { Component, OnInit, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ProdutorResumo, Saldo } from '../../../core/models';
import { SaldoService } from '../../../core/services/operacoes.service';
import { mensagemDeErro } from '../../../core/util';

export interface SaldosProdutorData {
  produtor: ProdutorResumo;
}

@Component({
  selector: 'app-saldos-produtor-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>Saldos de {{ data.produtor.nome }}</h2>
    <mat-dialog-content>
      @if (carregando()) {
        <mat-progress-bar mode="indeterminate" />
      }
      @if (erro(); as e) {
        <p class="erro-form" role="alert">{{ e }}</p>
      }
      @if (!carregando() && !erro()) {
        <div class="table-wrap">
          <table class="tabela">
            <thead>
              <tr>
                <th>Produto</th>
                <th class="num">Saldo</th>
                <th class="num">Reservado</th>
                <th class="num">Valor estimado</th>
              </tr>
            </thead>
            <tbody>
              @for (s of saldos(); track s.produtoId) {
                <tr>
                  <td>{{ s.produtoNome }}</td>
                  <td class="num">{{ s.quantidade | number: '1.0-2' }} sc</td>
                  <td class="num">{{ s.reservado | number: '1.0-2' }} sc</td>
                  <td class="num">{{ s.valorEstimado | currency: 'BRL' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Fechar</button>
    </mat-dialog-actions>
  `,
})
export class SaldosProdutorDialog implements OnInit {
  protected readonly data = inject<SaldosProdutorData>(MAT_DIALOG_DATA);
  private readonly api = inject(SaldoService);

  protected readonly saldos = signal<Saldo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  ngOnInit(): void {
    this.api.saldosDoProdutor(this.data.produtor.id).subscribe({
      next: (lista) => {
        this.saldos.set(lista);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar os saldos.'));
        this.carregando.set(false);
      },
    });
  }
}
