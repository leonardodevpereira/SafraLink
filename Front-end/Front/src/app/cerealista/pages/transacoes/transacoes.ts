import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { Produto, ProdutorResumo, StatusRequisicao, Transacao } from '../../../core/models';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { ProdutorService, RelatorioService } from '../../../core/services/gestao.service';
import { FiltroTransacoes, TransacaoService } from '../../../core/services/operacoes.service';
import { mensagemDeErro } from '../../../core/util';
import { TransacaoDetalheData, TransacaoDetalheDialog } from '../../../shared/transacao-detalhe/transacao-detalhe-dialog';

@Component({
  selector: 'app-transacoes',
  standalone: false,
  templateUrl: './transacoes.html',
  styleUrl: './transacoes.scss',
})
export class Transacoes implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(TransacaoService);
  private readonly produtorApi = inject(ProdutorService);
  private readonly catalogo = inject(CatalogoService);
  private readonly relatorios = inject(RelatorioService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  protected readonly situacoes: { valor: StatusRequisicao; rotulo: string }[] = [
    { valor: 'APROVADA', rotulo: 'Aprovada' },
    { valor: 'PROCESSADA', rotulo: 'Processada' },
  ];

  protected readonly produtores = signal<ProdutorResumo[]>([]);
  protected readonly produtos = signal<Produto[]>([]);
  protected readonly itens = signal<Transacao[]>([]);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(10);
  protected readonly total = signal(0);
  protected readonly carregando = signal(true);
  protected readonly exportando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly filtros = this.fb.group({
    produtorId: this.fb.control<number | null>(null),
    produtoId: this.fb.control<number | null>(null),
    status: this.fb.control<StatusRequisicao | null>(null),
    de: this.fb.nonNullable.control(''),
    ate: this.fb.nonNullable.control(''),
  });

  ngOnInit(): void {
    forkJoin({ produtores: this.produtorApi.listar(), produtos: this.catalogo.produtos() }).subscribe({
      next: ({ produtores, produtos }) => {
        this.produtores.set(produtores);
        this.produtos.set(produtos);
      },
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
    this.carregar();
  }

  protected aplicar(): void {
    this.pagina.set(0);
    this.carregar();
  }

  protected limpar(): void {
    this.filtros.reset({ de: '', ate: '' });
    this.aplicar();
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
    const { de, ate } = this.filtros.getRawValue();
    this.exportando.set(true);
    this.relatorios.baixarTransacoes(de || null, ate || null).subscribe({
      next: (blob) => {
        this.relatorios.salvar(blob, 'safralink-transacoes.csv');
        this.exportando.set(false);
      },
      error: (e: unknown) => {
        this.snackBar.open(mensagemDeErro(e, 'Não foi possível exportar.'), 'OK', { duration: 5000 });
        this.exportando.set(false);
      },
    });
  }

  private filtroAtual(): FiltroTransacoes {
    const v = this.filtros.getRawValue();
    return {
      produtorId: v.produtorId,
      produtoId: v.produtoId,
      status: v.status,
      de: v.de || null,
      ate: v.ate || null,
      pagina: this.pagina(),
      tamanho: this.tamanho(),
    };
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listar(this.filtroAtual()).subscribe({
      next: (p) => {
        this.itens.set(p.conteudo);
        this.total.set(p.total);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar as transações.'));
        this.carregando.set(false);
      },
    });
  }
}
