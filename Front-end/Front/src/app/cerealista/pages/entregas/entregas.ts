import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { Entrega, Produto, ProdutorResumo } from '../../../core/models';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { ProdutorService } from '../../../core/services/gestao.service';
import { EntregaService } from '../../../core/services/operacoes.service';
import { hojeISO, mensagemDeErro } from '../../../core/util';
import { ConfirmDialog, ConfirmacaoData } from '../../../shared/dialogs/dialogs';
import { EntregaDialog, EntregaDialogData } from './entrega-dialog';

@Component({
  selector: 'app-entregas',
  standalone: false,
  templateUrl: './entregas.html',
  styleUrl: './entregas.scss',
})
export class Entregas implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(EntregaService);
  private readonly produtorApi = inject(ProdutorService);
  private readonly catalogo = inject(CatalogoService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  protected readonly hoje = hojeISO();
  protected readonly produtores = signal<ProdutorResumo[]>([]);
  protected readonly produtos = signal<Produto[]>([]);
  protected readonly entregas = signal<Entrega[]>([]);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(10);
  protected readonly total = signal(0);
  protected readonly carregando = signal(true);
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = this.fb.group({
    produtorId: this.fb.control<number | null>(null, Validators.required),
    produtoId: this.fb.control<number | null>(null, Validators.required),
    quantidade: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    dataEntrega: this.fb.nonNullable.control(this.hoje, Validators.required),
    observacao: this.fb.nonNullable.control('', Validators.maxLength(255)),
  });

  ngOnInit(): void {
    forkJoin({ produtores: this.produtorApi.listar(), produtos: this.catalogo.produtos() }).subscribe({
      next: ({ produtores, produtos }) => {
        this.produtores.set(produtores.filter((p) => p.ativo));
        this.produtos.set(produtos);
      },
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
    this.carregar();
  }

  protected editar(entrega: Entrega): void {
    this.dialog
      .open<EntregaDialog, EntregaDialogData, boolean>(EntregaDialog, {
        data: { entrega },
        width: '440px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((salvou) => {
        if (salvou) {
          this.snackBar.open('Entrega atualizada.', 'OK', { duration: 4000 });
          this.carregar();
        }
      });
  }

  protected excluir(entrega: Entrega): void {
    this.dialog
      .open<ConfirmDialog, ConfirmacaoData, boolean>(ConfirmDialog, {
        data: {
          titulo: 'Excluir entrega',
          mensagem: `Excluir a entrega de ${entrega.quantidade} sc de ${entrega.produtoNome} de ${entrega.produtorNome}? O saldo do produtor será reduzido.`,
          confirmar: 'Excluir',
        },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.excluir(entrega.id).subscribe({
          next: () => {
            this.snackBar.open('Entrega excluída.', 'OK', { duration: 4000 });
            this.carregar();
          },
          error: (e: unknown) => this.snackBar.open(mensagemDeErro(e), 'OK', { duration: 6000 }),
        });
      });
  }

  protected mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  protected registrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.erro.set(null);
    this.api
      .registrar({
        produtorId: v.produtorId as number,
        produtoId: v.produtoId as number,
        quantidade: v.quantidade as number,
        dataEntrega: v.dataEntrega,
        observacao: v.observacao.trim() || undefined,
      })
      .subscribe({
        next: (e) => {
          this.snackBar.open(`${e.quantidade} sc de ${e.produtoNome} creditadas a ${e.produtorNome}.`, 'OK', { duration: 5000 });
          this.form.reset({ dataEntrega: this.hoje, observacao: '' });
          this.salvando.set(false);
          this.pagina.set(0);
          this.carregar();
        },
        error: (e: unknown) => {
          this.erro.set(mensagemDeErro(e));
          this.salvando.set(false);
        },
      });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.api.listar(this.pagina(), this.tamanho()).subscribe({
      next: (p) => {
        this.entregas.set(p.conteudo);
        this.total.set(p.total);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar as entregas.'));
        this.carregando.set(false);
      },
    });
  }
}
