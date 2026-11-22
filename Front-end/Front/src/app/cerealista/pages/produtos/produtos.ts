import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Produto } from '../../../core/models';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { mensagemDeErro } from '../../../core/util';
import { ConfirmDialog, ConfirmacaoData } from '../../../shared/dialogs/dialogs';
import { ProdutoDialog, ProdutoDialogData } from './produto-dialog';

@Component({
  selector: 'app-cadastro-produtos',
  standalone: false,
  templateUrl: './produtos.html',
  styleUrl: './produtos.scss',
})
export class CadastroProdutos implements OnInit {
  private readonly api = inject(CatalogoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly produtos = signal<Produto[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  ngOnInit(): void {
    this.carregar();
  }

  protected abrir(produto?: Produto): void {
    this.dialog
      .open<ProdutoDialog, ProdutoDialogData, boolean>(ProdutoDialog, {
        data: { produto },
        width: '440px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((salvou) => {
        if (salvou) {
          this.snackBar.open('Produto salvo.', 'OK', { duration: 4000 });
          this.carregar();
        }
      });
  }

  protected excluir(produto: Produto): void {
    this.dialog
      .open<ConfirmDialog, ConfirmacaoData, boolean>(ConfirmDialog, {
        data: {
          titulo: 'Excluir produto',
          mensagem: `Excluir "${produto.nome}"? Produtos com preços, entregas, saldos ou vendas não podem ser excluídos.`,
          confirmar: 'Excluir',
        },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.excluirProduto(produto.id).subscribe({
          next: () => {
            this.snackBar.open('Produto excluído.', 'OK', { duration: 4000 });
            this.carregar();
          },
          error: (e: unknown) => this.snackBar.open(mensagemDeErro(e), 'OK', { duration: 6000 }),
        });
      });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.produtos().subscribe({
      next: (lista) => {
        this.produtos.set(lista);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar os produtos.'));
        this.carregando.set(false);
      },
    });
  }
}
