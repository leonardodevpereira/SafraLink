import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { DashboardCerealista, Requisicao } from '../../../core/models';
import { DashboardService } from '../../../core/services/gestao.service';
import { mensagemDeErro } from '../../../core/util';
import {
  RequisicaoDetalheData,
  RequisicaoDetalheDialog,
} from '../../../shared/requisicao-detalhe/requisicao-detalhe';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const sacas = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });
const diaMes = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', timeZone: 'UTC' });

@Component({
  selector: 'app-dashboard',
  standalone: false,
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly api = inject(DashboardService);
  private readonly dialog = inject(MatDialog);

  protected readonly dados = signal<DashboardCerealista | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly maiorValor = computed(() =>
    Math.max(1, ...(this.dados()?.comprasPorProduto.map((c) => c.valor) ?? [0])),
  );

  protected readonly dias = computed(() => {
    const serie = this.dados()?.comprasPorDia ?? [];
    const maior = Math.max(1, ...serie.map((d) => d.valor));
    return serie.map((d) => ({
      altura: d.valor > 0 ? Math.max(4, (d.valor / maior) * 100) : 0,
      texto: `${diaMes.format(new Date(d.data))}: ${d.valor > 0 ? `${moeda.format(d.valor)} · ${sacas.format(d.sacas)} sc` : 'sem compras'}`,
      rotulo: diaMes.format(new Date(d.data)),
      temCompra: d.valor > 0,
    }));
  });

  protected readonly totalDoPeriodo = computed(() =>
    (this.dados()?.comprasPorDia ?? []).reduce((total, d) => total + d.valor, 0),
  );

  ngOnInit(): void {
    this.carregar();
  }

  protected abrir(requisicao: Requisicao): void {
    this.dialog
      .open<RequisicaoDetalheDialog, RequisicaoDetalheData, boolean>(RequisicaoDetalheDialog, {
        data: { requisicao },
        width: '640px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((alterou) => {
        if (alterou) {
          this.carregar();
        }
      });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.daCerealista().subscribe({
      next: (d) => {
        this.dados.set(d);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar o painel.'));
        this.carregando.set(false);
      },
    });
  }
}
