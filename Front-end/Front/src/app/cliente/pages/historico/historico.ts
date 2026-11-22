import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Entrega, Transacao } from '../../../core/models';
import { RelatorioService } from '../../../core/services/gestao.service';
import { EntregaService, TransacaoService } from '../../../core/services/operacoes.service';
import { mensagemDeErro } from '../../../core/util';
import {
  TransacaoDetalheData,
  TransacaoDetalheDialog,
} from '../../../shared/transacao-detalhe/transacao-detalhe-dialog';

type Aba = 'vendas' | 'entregas';

@Component({
  selector: 'app-historico',
  standalone: false,
  templateUrl: './historico.html',
  styleUrl: './historico.scss',
})
export class Historico implements OnInit {
  private readonly transacaoApi = inject(TransacaoService);
  private readonly entregaApi = inject(EntregaService);
  private readonly relatorios = inject(RelatorioService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  protected readonly aba = signal<Aba>('vendas');
  protected readonly transacoes = signal<Transacao[]>([]);
  protected readonly entregas = signal<Entrega[]>([]);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(10);
  protected readonly total = signal(0);
  protected readonly de = signal('');
  protected readonly ate = signal('');
  protected readonly carregando = signal(true);
  protected readonly exportando = signal(false);
  protected readonly erro = signal<string | null>(null);

  ngOnInit(): void {
    this.carregar();
  }

  protected trocarAba(aba: Aba): void {
    this.aba.set(aba);
    this.pagina.set(0);
    this.carregar();
  }

  protected aplicarPeriodo(de: string, ate: string): void {
    this.de.set(de);
    this.ate.set(ate);
    this.pagina.set(0);
    this.carregar();
  }

  protected mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  protected abrir(transacao: Transacao): void {
    this.dialog.open<TransacaoDetalheDialog, TransacaoDetalheData>(TransacaoDetalheDialog, {
      data: { transacao },
      width: '760px',
      maxWidth: '95vw',
    });
  }

  protected descricaoDescontos(t: Transacao): string {
    const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    return t.descontos.map((d) => `${d.nome}: ${moeda.format(d.valor)}`).join(' | ');
  }

  protected exportar(): void {
    this.exportando.set(true);
    this.relatorios.baixarTransacoes(this.de() || null, this.ate() || null).subscribe({
      next: (blob) => {
        this.relatorios.salvar(blob, 'safralink-minhas-vendas.csv');
        this.exportando.set(false);
      },
      error: (e: unknown) => {
        this.snackBar.open(mensagemDeErro(e, 'Não foi possível exportar o relatório.'), 'OK', { duration: 5000 });
        this.exportando.set(false);
      },
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    const falha = (e: unknown): void => {
      this.erro.set(mensagemDeErro(e, 'Não foi possível carregar o histórico.'));
      this.carregando.set(false);
    };
    if (this.aba() === 'vendas') {
      this.transacaoApi
        .listar({ de: this.de() || null, ate: this.ate() || null, pagina: this.pagina(), tamanho: this.tamanho() })
        .subscribe({
          next: (p) => {
            this.transacoes.set(p.conteudo);
            this.total.set(p.total);
            this.carregando.set(false);
          },
          error: falha,
        });
    } else {
      this.entregaApi.listar(this.pagina(), this.tamanho()).subscribe({
        next: (p) => {
          this.entregas.set(p.conteudo);
          this.total.set(p.total);
          this.carregando.set(false);
        },
        error: falha,
      });
    }
  }
}
