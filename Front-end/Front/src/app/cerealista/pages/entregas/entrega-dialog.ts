import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Entrega } from '../../../core/models';
import { EntregaService } from '../../../core/services/operacoes.service';
import { hojeISO, mensagemDeErro } from '../../../core/util';

export interface EntregaDialogData {
  entrega: Entrega;
}

@Component({
  selector: 'app-entrega-dialog',
  standalone: false,
  templateUrl: './entrega-dialog.html',
})
export class EntregaDialog {
  protected readonly data = inject<EntregaDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<EntregaDialog, boolean>>(MatDialogRef);
  private readonly api = inject(EntregaService);
  private readonly fb = inject(FormBuilder);

  protected readonly hoje = hojeISO();
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    quantidade: [this.data.entrega.quantidade as number | null, [Validators.required, Validators.min(0.01)]],
    dataEntrega: [this.data.entrega.dataEntrega, Validators.required],
    observacao: [this.data.entrega.observacao ?? '', Validators.maxLength(255)],
  });

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.erro.set(null);
    this.api
      .atualizar(this.data.entrega.id, {
        quantidade: v.quantidade as number,
        dataEntrega: v.dataEntrega,
        observacao: v.observacao.trim() || undefined,
      })
      .subscribe({
        next: () => this.ref.close(true),
        error: (e: unknown) => {
          this.erro.set(mensagemDeErro(e));
          this.salvando.set(false);
        },
      });
  }
}
