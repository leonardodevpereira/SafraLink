import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Produto, Requisicao, Saldo } from '../../../core/models';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { SaldoService } from '../../../core/services/operacoes.service';
import { mensagemDeErro } from '../../../core/util';
import {
  SolicitacaoPersonalizadaData,
  SolicitacaoPersonalizadaDialog,
} from './solicitacao-personalizada-dialog';
import { SolicitarVendaData, SolicitarVendaDialog } from './solicitar-venda-dialog';

@Component({
  selector: 'app-produtos',
  standalone: false,
  templateUrl: './produtos.html',
  styleUrl: './produtos.scss',
})
export class Produtos implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly saldoApi = inject(SaldoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly produtos = signal<Produto[]>([]);
  private readonly saldos = signal<Saldo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly disponivel = computed(() => {
    const mapa = new Map<number, number>();
    this.saldos().forEach((s) => mapa.set(s.produtoId, s.disponivelParaVenda));
    return mapa;
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected podeVender(p: Produto): boolean {
    return (this.disponivel().get(p.id) ?? 0) > 0 && p.precoAtual !== null && p.precoAtual !== undefined;
  }

  protected readonly opcoes = computed(() =>
    this.produtos()
      .filter((p) => this.podeVender(p))
      .map((produto) => ({ produto, disponivel: this.disponivel().get(produto.id) ?? 0 })),
  );

  protected vender(produto: Produto): void {
    this.dialog
      .open<SolicitarVendaDialog, SolicitarVendaData, Requisicao>(SolicitarVendaDialog, {
        data: { produto, disponivel: this.disponivel().get(produto.id) ?? 0 },
        width: '460px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((criada) => this.aposCriar(criada));
  }

  protected personalizada(): void {
    this.dialog
      .open<SolicitacaoPersonalizadaDialog, SolicitacaoPersonalizadaData, Requisicao>(SolicitacaoPersonalizadaDialog, {
        data: { opcoes: this.opcoes() },
        width: '560px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((criada) => this.aposCriar(criada));
  }

  private aposCriar(criada: Requisicao | undefined): void {
    if (!criada) {
      return;
    }
    this.carregar();
    this.snackBar
      .open('Solicitação enviada para a cerealista.', 'Ver', { duration: 6000 })
      .onAction()
      .subscribe(() => void this.router.navigateByUrl('/cliente/requisicoes'));
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    forkJoin({ produtos: this.catalogo.produtos(), saldos: this.saldoApi.meusSaldos() }).subscribe({
      next: ({ produtos, saldos }) => {
        this.produtos.set(produtos);
        this.saldos.set(saldos);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar os produtos.'));
        this.carregando.set(false);
      },
    });
  }
}
