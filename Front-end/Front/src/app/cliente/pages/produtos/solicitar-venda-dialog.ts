import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Produto, Requisicao } from '../../../core/models';
import { RequisicaoService } from '../../../core/services/operacoes.service';
import { mensagemDeErro } from '../../../core/util';

export interface SolicitarVendaData {
  produto: Produto;
  disponivel: number;
}

@Component({
  selector: 'app-solicitar-venda-dialog',
  standalone: false,
  templateUrl: './solicitar-venda-dialog.html',
  styleUrl: './solicitar-venda-dialog.scss',
})
export class SolicitarVendaDialog {
  protected readonly data = inject<SolicitarVendaData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<SolicitarVendaDialog, Requisicao>>(MatDialogRef);
  private readonly api = inject(RequisicaoService);
  private readonly fb = inject(FormBuilder);

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = this.fb.group({
    quantidade: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      Validators.max(this.data.disponivel),
    ]),
    precoSaca: this.fb.control<number | null>(this.data.produto.precoAtual ?? null, [
      Validators.required,
      Validators.min(0.01),
    ]),
  });

  private readonly valores = toSignal(this.form.valueChanges, { initialValue: this.form.value });

  protected readonly total = computed(() => {
    const v = this.valores();
    return v.quantidade && v.precoSaca ? v.quantidade * v.precoSaca : null;
  });

  protected readonly precoDiferente = computed(() => {
    const preco = this.valores().precoSaca;
    const atual = this.data.produto.precoAtual;
    return preco != null && atual != null && Math.abs(preco - atual) > 0.0001;
  });

  protected readonly acimaDaCerealista = computed(() => {
    const qtd = this.valores().quantidade;
    return qtd != null && qtd > this.data.produto.quantidadeDisponivel;
  });

  protected usarSaldoTotal(): void {
    this.form.controls.quantidade.setValue(this.data.disponivel);
  }

  protected confirmar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { quantidade, precoSaca } = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set(null);
    const preco = this.precoDiferente() ? precoSaca : undefined;
    this.api.criar(this.data.produto.id, quantidade as number, preco).subscribe({
      next: (r) => this.ref.close(r),
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }
}
