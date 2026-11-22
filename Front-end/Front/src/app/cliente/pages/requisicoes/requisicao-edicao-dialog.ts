import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Requisicao } from '../../../core/models';
import { RequisicaoService } from '../../../core/services/operacoes.service';
import { mensagemDeErro } from '../../../core/util';

export interface RequisicaoEdicaoData {
  requisicao: Requisicao;
}

@Component({
  selector: 'app-requisicao-edicao-dialog',
  standalone: false,
  templateUrl: './requisicao-edicao-dialog.html',
})
export class RequisicaoEdicaoDialog {
  protected readonly data = inject<RequisicaoEdicaoData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<RequisicaoEdicaoDialog, boolean>>(MatDialogRef);
  private readonly api = inject(RequisicaoService);
  private readonly fb = inject(FormBuilder);

  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    quantidade: [this.data.requisicao.quantidade as number | null, [Validators.required, Validators.min(0.01)]],
    precoSaca: [this.data.requisicao.precoSaca as number | null, [Validators.required, Validators.min(0.01)]],
    observacao: [this.data.requisicao.observacao ?? '', Validators.maxLength(255)],
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
      .atualizar(this.data.requisicao.id, {
        quantidade: v.quantidade as number,
        precoSaca: v.precoSaca,
        observacao: v.observacao,
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
