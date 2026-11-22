import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { Requisicao, StatusRequisicao } from '../../../core/models';
import { RequisicaoService } from '../../../core/services/operacoes.service';
import { mensagemDeErro } from '../../../core/util';
import { ConfirmDialog, ConfirmacaoData } from '../../../shared/dialogs/dialogs';
import { RequisicaoEdicaoData, RequisicaoEdicaoDialog } from './requisicao-edicao-dialog';
import {
  RequisicaoDetalheData,
  RequisicaoDetalheDialog,
} from '../../../shared/requisicao-detalhe/requisicao-detalhe';

@Component({
  selector: 'app-requisicoes',
  standalone: false,
  templateUrl: './requisicoes.html',
  styleUrl: './requisicoes.scss',
})
export class Requisicoes implements OnInit {
  private readonly api = inject(RequisicaoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly filtros: { valor: StatusRequisicao | null; rotulo: string }[] = [
    { valor: null, rotulo: 'Todas' },
    { valor: 'PENDENTE', rotulo: 'Pendentes' },
    { valor: 'APROVADA', rotulo: 'Aprovadas' },
    { valor: 'RECUSADA', rotulo: 'Recusadas' },
    { valor: 'PROCESSADA', rotulo: 'Processadas' },
  ];

  protected readonly itens = signal<Requisicao[]>([]);
  protected readonly status = signal<StatusRequisicao | null>(null);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(10);
  protected readonly total = signal(0);
  protected readonly carregando = signal(true);
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

  protected temPropostaParaMim(r: Requisicao): boolean {
    return r.status === 'PENDENTE' && r.contraPropostas.some((c) => c.status === 'PENDENTE' && c.autorPerfil === 'CEREALISTA');
  }

  protected editar(r: Requisicao): void {
    this.dialog
      .open<RequisicaoEdicaoDialog, RequisicaoEdicaoData, boolean>(RequisicaoEdicaoDialog, {
        data: { requisicao: r },
        width: '460px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((salvou) => {
        if (salvou) {
          this.snackBar.open('Solicitação atualizada.', 'OK', { duration: 4000 });
          this.carregar();
        }
      });
  }

  protected cancelar(r: Requisicao): void {
    this.dialog
      .open<ConfirmDialog, ConfirmacaoData, boolean>(ConfirmDialog, {
        data: {
          titulo: 'Cancelar solicitação',
          mensagem: `Cancelar a solicitação de ${r.quantidade} sc de ${r.produtoNome}? Ela será excluída.`,
          confirmar: 'Cancelar solicitação',
          cancelar: 'Voltar',
        },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.excluir(r.id).subscribe({
          next: () => {
            this.snackBar.open('Solicitação cancelada.', 'OK', { duration: 4000 });
            this.carregar();
          },
          error: (e: unknown) => this.snackBar.open(mensagemDeErro(e), 'OK', { duration: 6000 }),
        });
      });
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
    this.api.listar({ status: this.status(), pagina: this.pagina(), tamanho: this.tamanho() }).subscribe({
      next: (p) => {
        this.itens.set(p.conteudo);
        this.total.set(p.total);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar suas requisições.'));
        this.carregando.set(false);
      },
    });
  }
}
