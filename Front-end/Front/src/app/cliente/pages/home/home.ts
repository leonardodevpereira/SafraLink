import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { DashboardProdutor, Requisicao, TipoGrao } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { DashboardService } from '../../../core/services/gestao.service';
import { mensagemDeErro } from '../../../core/util';
import {
  RequisicaoDetalheData,
  RequisicaoDetalheDialog,
} from '../../../shared/requisicao-detalhe/requisicao-detalhe';

const NOMES: Record<TipoGrao, string> = { SOJA: 'Soja', MILHO: 'Milho', FEIJAO: 'Feijão' };

interface ResumoGrao {
  grao: TipoGrao;
  nome: string;
  saldo: number;
  valor: number;
  reservado: number;
  mercados: { rotulo: string; saldo: number }[];
}

@Component({
  selector: 'app-home',
  standalone: false,
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit {
  private readonly dashboard = inject(DashboardService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);

  protected readonly dados = signal<DashboardProdutor | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly primeiroNome = computed(() => this.auth.usuario()?.nome.split(' ')[0] ?? '');

  protected readonly graos = computed<ResumoGrao[]>(() => {
    const d = this.dados();
    if (!d) {
      return [];
    }
    const produtoPorId = new Map(d.precos.map((p) => [p.id, p]));
    const ordem: TipoGrao[] = ['SOJA', 'MILHO', 'FEIJAO'];
    return ordem.map((grao) => {
      const itens = d.saldos
        .map((saldo) => ({ saldo, produto: produtoPorId.get(saldo.produtoId) }))
        .filter((x) => x.produto?.grao === grao);
      return {
        grao,
        nome: NOMES[grao],
        saldo: itens.reduce((t, x) => t + x.saldo.quantidade, 0),
        valor: itens.reduce((t, x) => t + x.saldo.valorEstimado, 0),
        reservado: itens.reduce((t, x) => t + x.saldo.reservado, 0),
        mercados: itens.map((x) => ({
          rotulo: x.produto?.mercado === 'EXTERNO' ? 'Externo' : 'Interno',
          saldo: x.saldo.quantidade,
        })),
      };
    });
  });

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
    this.dashboard.doProdutor().subscribe({
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
