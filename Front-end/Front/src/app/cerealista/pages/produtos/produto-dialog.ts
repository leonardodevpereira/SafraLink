import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Mercado, Produto, TipoGrao } from '../../../core/models';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { mensagemDeErro } from '../../../core/util';

export interface ProdutoDialogData {
  produto?: Produto;
}

@Component({
  selector: 'app-produto-dialog',
  standalone: false,
  templateUrl: './produto-dialog.html',
})
export class ProdutoDialog {
  private readonly data = inject<ProdutoDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<ProdutoDialog, boolean>>(MatDialogRef);
  private readonly api = inject(CatalogoService);
  private readonly fb = inject(FormBuilder);

  protected readonly edicao = !!this.data.produto;
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly graos: { valor: TipoGrao; rotulo: string }[] = [
    { valor: 'SOJA', rotulo: 'Soja' },
    { valor: 'MILHO', rotulo: 'Milho' },
    { valor: 'FEIJAO', rotulo: 'Feijão' },
  ];
  protected readonly mercados: { valor: Mercado; rotulo: string }[] = [
    { valor: 'INTERNO', rotulo: 'Interno' },
    { valor: 'EXTERNO', rotulo: 'Externo' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    nome: [this.data.produto?.nome ?? '', [Validators.required, Validators.maxLength(60)]],
    grao: [this.data.produto?.grao ?? ('SOJA' as TipoGrao), Validators.required],
    mercado: [this.data.produto?.mercado ?? ('INTERNO' as Mercado), Validators.required],
  });

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req = { nome: v.nome.trim(), grao: v.grao, mercado: v.mercado };
    this.salvando.set(true);
    this.erro.set(null);
    const chamada = this.data.produto ? this.api.atualizarProduto(this.data.produto.id, req) : this.api.criarProduto(req);
    chamada.subscribe({
      next: () => this.ref.close(true),
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.salvando.set(false);
      },
    });
  }
}
