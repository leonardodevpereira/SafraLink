import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ProdutorResumo } from '../../../core/models';
import { ProdutorService } from '../../../core/services/gestao.service';
import { mensagemDeErro, somenteDigitos, validarCpf } from '../../../core/util';

export interface ProdutorDialogData {
  produtor?: ProdutorResumo;
}

@Component({
  selector: 'app-produtor-dialog',
  standalone: false,
  templateUrl: './produtor-dialog.html',
})
export class ProdutorDialog {
  private readonly data = inject<ProdutorDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<ProdutorDialog, boolean>>(MatDialogRef);
  private readonly api = inject(ProdutorService);
  private readonly fb = inject(FormBuilder);

  protected readonly edicao = !!this.data.produtor;
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    nome: [this.data.produtor?.nome ?? '', [Validators.required, Validators.maxLength(150)]],
    email: [this.data.produtor?.email ?? '', [Validators.required, Validators.email, Validators.maxLength(180)]],
    cpf: [this.data.produtor?.cpf ?? '', this.edicao ? [] : [Validators.required, validarCpf]],
    telefone: [this.data.produtor?.telefone ?? '', Validators.maxLength(20)],
    senha: ['', this.edicao ? [] : [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  constructor() {
    if (this.edicao) {
      this.form.controls.cpf.disable();
    }
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.erro.set(null);
    const chamada = this.data.produtor
      ? this.api.atualizar(this.data.produtor.id, {
          nome: v.nome.trim(),
          email: v.email.trim(),
          telefone: v.telefone.trim() || undefined,
        })
      : this.api.criar({
          nome: v.nome.trim(),
          email: v.email.trim(),
          senha: v.senha,
          cpf: somenteDigitos(v.cpf),
          telefone: v.telefone.trim() || undefined,
        });
    chamada.subscribe({
      next: () => this.ref.close(true),
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.salvando.set(false);
      },
    });
  }
}
