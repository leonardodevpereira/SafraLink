import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Desconto } from '../../../core/models';
import { DescontoService } from '../../../core/services/gestao.service';
import { mensagemDeErro } from '../../../core/util';
import { ConfirmDialog, ConfirmacaoData } from '../../../shared/dialogs/dialogs';
import { DescontoDialog, DescontoDialogData } from './desconto-dialog';

@Component({
  selector: 'app-descontos',
  standalone: false,
  templateUrl: './descontos.html',
  styleUrl: './descontos.scss',
})
export class Descontos implements OnInit {
  private readonly api = inject(DescontoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly descontos = signal<Desconto[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  ngOnInit(): void {
    this.carregar();
  }

  protected rotuloCategoria(d: Desconto): string {
    return { ARMAZENAGEM: 'Armazenagem', TRANSPORTE: 'Transporte', OUTRO: 'Outro' }[d.categoria];
  }

  protected abrir(desconto?: Desconto): void {
    this.dialog
      .open<DescontoDialog, DescontoDialogData, boolean>(DescontoDialog, {
        data: { desconto },
        width: '460px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((salvou) => {
        if (salvou) {
          this.snackBar.open('Desconto salvo.', 'OK', { duration: 4000 });
          this.carregar();
        }
      });
  }

  protected excluir(d: Desconto): void {
    this.dialog
      .open<ConfirmDialog, ConfirmacaoData, boolean>(ConfirmDialog, {
        data: {
          titulo: 'Excluir desconto',
          mensagem: `Excluir "${d.nome}"? Descontos já aplicados em vendas não podem ser excluídos; nesse caso, desative-o.`,
          confirmar: 'Excluir',
        },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.excluir(d.id).subscribe({
          next: () => {
            this.snackBar.open('Desconto excluído.', 'OK', { duration: 4000 });
            this.carregar();
          },
          error: (e: unknown) => this.snackBar.open(mensagemDeErro(e), 'OK', { duration: 6000 }),
        });
      });
  }

  protected alternar(d: Desconto, ativo: boolean): void {
    this.api.atualizar(d.id, { nome: d.nome, categoria: d.categoria, forma: d.forma, valor: d.valor, ativo }).subscribe({
      next: () => {
        this.snackBar.open(ativo ? 'Desconto ativado.' : 'Desconto desativado.', 'OK', { duration: 3000 });
        this.carregar();
      },
      error: (e: unknown) => {
        this.snackBar.open(mensagemDeErro(e), 'OK', { duration: 5000 });
        this.carregar();
      },
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listar().subscribe({
      next: (lista) => {
        this.descontos.set(lista);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar os descontos.'));
        this.carregando.set(false);
      },
    });
  }
}
