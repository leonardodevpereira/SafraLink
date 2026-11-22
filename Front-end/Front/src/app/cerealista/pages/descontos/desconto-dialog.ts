import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CategoriaDesconto, Desconto, FormaDesconto } from '../../../core/models';
import { DescontoService } from '../../../core/services/gestao.service';
import { mensagemDeErro } from '../../../core/util';

export interface DescontoDialogData {
  desconto?: Desconto;
}

@Component({
  selector: 'app-desconto-dialog',
  standalone: false,
  templateUrl: './desconto-dialog.html',
})
export class DescontoDialog {
  private readonly data = inject<DescontoDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<DescontoDialog, boolean>>(MatDialogRef);
  private readonly api = inject(DescontoService);
  private readonly fb = inject(FormBuilder);

  protected readonly edicao = !!this.data.desconto;
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly categorias: { valor: CategoriaDesconto; rotulo: string }[] = [
    { valor: 'ARMAZENAGEM', rotulo: 'Armazenagem' },
    { valor: 'TRANSPORTE', rotulo: 'Transporte' },
    { valor: 'OUTRO', rotulo: 'Outro' },
  ];

  protected readonly formas: { valor: FormaDesconto; rotulo: string }[] = [
    { valor: 'PERCENTUAL', rotulo: 'Percentual sobre o valor bruto' },
    { valor: 'VALOR_FIXO', rotulo: 'Valor fixo (R$ por saca)' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    nome: [this.data.desconto?.nome ?? '', [Validators.required, Validators.maxLength(80)]],
    categoria: [this.data.desconto?.categoria ?? ('ARMAZENAGEM' as CategoriaDesconto), Validators.required],
    forma: [this.data.desconto?.forma ?? ('PERCENTUAL' as FormaDesconto), Validators.required],
    valor: [this.data.desconto?.valor ?? (null as number | null), [Validators.required, Validators.min(0)]],
    ativo: [this.data.desconto?.ativo ?? true],
  });

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (v.forma === 'PERCENTUAL' && (v.valor ?? 0) > 100) {
      this.erro.set('O percentual não pode passar de 100%.');
      return;
    }
    const req = { nome: v.nome.trim(), categoria: v.categoria, forma: v.forma, valor: v.valor as number, ativo: v.ativo };
    this.salvando.set(true);
    this.erro.set(null);
    const chamada = this.data.desconto ? this.api.atualizar(this.data.desconto.id, req) : this.api.criar(req);
    chamada.subscribe({
      next: () => this.ref.close(true),
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.salvando.set(false);
      },
    });
  }
}
