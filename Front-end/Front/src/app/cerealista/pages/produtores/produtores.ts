import { Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs';
import { ProdutorResumo } from '../../../core/models';
import { ProdutorService, RelatorioService } from '../../../core/services/gestao.service';
import { mensagemDeErro } from '../../../core/util';
import { ConfirmDialog, ConfirmacaoData } from '../../../shared/dialogs/dialogs';
import { ProdutorDialog, ProdutorDialogData } from './produtor-dialog';
import { SaldosProdutorData, SaldosProdutorDialog } from './saldos-produtor-dialog';

@Component({
  selector: 'app-produtores',
  standalone: false,
  templateUrl: './produtores.html',
  styleUrl: './produtores.scss',
})
export class Produtores implements OnInit {
  private readonly api = inject(ProdutorService);
  private readonly relatorios = inject(RelatorioService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly busca = new FormControl('', { nonNullable: true });
  protected readonly produtores = signal<ProdutorResumo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly exportando = signal(false);
  protected readonly erro = signal<string | null>(null);

  constructor() {
    this.busca.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap(() => this.carregando.set(true)),
        switchMap((termo) => this.api.listar(termo)),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (lista) => this.mostrar(lista),
        error: (e: unknown) => this.falhar(e),
      });
  }

  ngOnInit(): void {
    this.api.listar().subscribe({
      next: (lista) => this.mostrar(lista),
      error: (e: unknown) => this.falhar(e),
    });
  }

  protected novoOuEditar(produtor?: ProdutorResumo): void {
    this.dialog
      .open<ProdutorDialog, ProdutorDialogData, boolean>(ProdutorDialog, {
        data: { produtor },
        width: '460px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((salvou) => {
        if (salvou) {
          this.snackBar.open('Produtor salvo.', 'OK', { duration: 4000 });
          this.recarregar();
        }
      });
  }

  protected excluir(produtor: ProdutorResumo): void {
    this.dialog
      .open<ConfirmDialog, ConfirmacaoData, boolean>(ConfirmDialog, {
        data: {
          titulo: 'Excluir produtor',
          mensagem: `Excluir ${produtor.nome}? O histórico é preservado, mas ele deixa de acessar o sistema. Produtores com saldo ou vendas pendentes não podem ser excluídos.`,
          confirmar: 'Excluir',
        },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.excluir(produtor.id).subscribe({
          next: () => {
            this.snackBar.open('Produtor excluído.', 'OK', { duration: 4000 });
            this.recarregar();
          },
          error: (e: unknown) => this.snackBar.open(mensagemDeErro(e), 'OK', { duration: 6000 }),
        });
      });
  }

  protected verSaldos(produtor: ProdutorResumo): void {
    this.dialog.open<SaldosProdutorDialog, SaldosProdutorData>(SaldosProdutorDialog, {
      data: { produtor },
      width: '560px',
      maxWidth: '95vw',
    });
  }

  protected exportar(): void {
    this.exportando.set(true);
    this.relatorios.baixarProdutores().subscribe({
      next: (blob) => {
        this.relatorios.salvar(blob, 'safralink-produtores.csv');
        this.exportando.set(false);
      },
      error: (e: unknown) => {
        this.snackBar.open(mensagemDeErro(e, 'Não foi possível exportar.'), 'OK', { duration: 5000 });
        this.exportando.set(false);
      },
    });
  }

  private recarregar(): void {
    this.carregando.set(true);
    this.api.listar(this.busca.value).subscribe({
      next: (lista) => this.mostrar(lista),
      error: (e: unknown) => this.falhar(e),
    });
  }

  private mostrar(lista: ProdutorResumo[]): void {
    this.produtores.set(lista);
    this.carregando.set(false);
    this.erro.set(null);
  }

  private falhar(e: unknown): void {
    this.erro.set(mensagemDeErro(e, 'Não foi possível carregar os produtores.'));
    this.carregando.set(false);
  }
}
