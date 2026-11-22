import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Saldo } from '../../../core/models';
import { SaldoService } from '../../../core/services/operacoes.service';
import { mensagemDeErro } from '../../../core/util';

@Component({
  selector: 'app-saldos',
  standalone: false,
  templateUrl: './saldos.html',
  styleUrl: './saldos.scss',
})
export class Saldos implements OnInit {
  private readonly api = inject(SaldoService);

  protected readonly saldos = signal<Saldo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly totalSacas = computed(() => this.saldos().reduce((t, s) => t + s.quantidade, 0));
  protected readonly totalValor = computed(() => this.saldos().reduce((t, s) => t + s.valorEstimado, 0));

  ngOnInit(): void {
    this.api.meusSaldos().subscribe({
      next: (lista) => {
        this.saldos.set(lista);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar seus saldos.'));
        this.carregando.set(false);
      },
    });
  }
}
