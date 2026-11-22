import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Requisicao, StatusRequisicao } from '../../../core/models';
import { RequisicaoService } from '../../../core/services/operacoes.service';
import { mensagemDeErro } from '../../../core/util';
import { ConfirmDialog, PromptDialog } from '../../../shared/dialogs/dialogs';
import {
  RequisicaoDetalheData,
  RequisicaoDetalheDialog,
} from '../../../shared/requisicao-detalhe/requisicao-detalhe';

@Component({
  selector: 'app-fila-requisicoes',
  standalone: false,
  templateUrl: './fila-requisicoes.html',
  styleUrl: './fila-requisicoes.scss',
})
export class FilaRequisicoes implements OnInit {
  private readonly api = inject(RequisicaoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly filtros: { valor: StatusRequisicao | null; rotulo: string }[] = [
    { valor: 'PENDENTE', rotulo: 'Pendentes' },
    { valor: 'APROVADA', rotulo: 'Aprovadas' },
    { valor: 'PROCESSADA', rotulo: 'Processadas' },
    { valor: 'RECUSADA', rotulo: 'Recusadas' },
    { valor: null, rotulo: 'Todas' },
  ];

  protected readonly itens = signal<Requisicao[]>([]);
  protected readonly status = signal<StatusRequisicao | null>('PENDENTE');
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(10);
  protected readonly total = signal(0);
  protected readonly carregando = signal(true);
  protected readonly ocupadoId = signal<number | null>(null);
  protected readonly erro = signal<string | null>(null);

  ngOnInit(): void {
    this.carregar();
  }

  protected filtrar(status: StatusRequisicao | null): void {
    this.status.set(status);
    this.pagina.set(0);
    this.carregar();
  }

  protected mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
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

  protected aprovar(r: Requisicao): void {
    const total = r.valorBruto.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    this.dialog
      .open<ConfirmDialog, { titulo: string; mensagem: string; confirmar: string }, boolean>(ConfirmDialog, {
        data: {
          titulo: 'Aprovar venda',
          mensagem: `Aprovar a compra de ${r.quantidade} sc de ${r.produtoNome} de ${r.produtorNome} por ${total}? O saldo do produtor será debitado e os descontos aplicados.`,
          confirmar: 'Aprovar',
        },
        width: '440px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.executar(r.id, this.api.aprovar(r.id), 'Venda aprovada.');
        }
      });
  }

  protected recusar(r: Requisicao): void {
    this.dialog
      .open<PromptDialog, { titulo: string; rotulo: string; confirmar: string }, string>(PromptDialog, {
        data: { titulo: 'Recusar venda', rotulo: 'Motivo da recusa', confirmar: 'Recusar' },
        width: '420px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((motivo) => {
        if (motivo) {
          this.executar(r.id, this.api.recusar(r.id, motivo), 'Venda recusada.');
        }
      });
  }

  private executar(id: number, chamada: ReturnType<RequisicaoService['aprovar']>, sucesso: string): void {
    this.ocupadoId.set(id);
    chamada.subscribe({
      next: () => {
        this.snackBar.open(sucesso, 'OK', { duration: 4000 });
        this.ocupadoId.set(null);
        this.carregar();
      },
      error: (e: unknown) => {
        this.snackBar.open(mensagemDeErro(e), 'OK', { duration: 7000 });
        this.ocupadoId.set(null);
      },
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listar({ status: this.status(), pagina: this.pagina(), tamanho: this.tamanho() }).subscribe({
      next: (p) => {
        this.itens.set(p.conteudo);
        this.total.set(p.total);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar as requisições.'));
        this.carregando.set(false);
      },
    });
  }
}
