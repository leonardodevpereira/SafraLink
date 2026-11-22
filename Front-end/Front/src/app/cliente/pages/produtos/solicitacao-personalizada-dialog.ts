import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Produto, Requisicao } from '../../../core/models';
import { RequisicaoService } from '../../../core/services/operacoes.service';
import { mensagemDeErro } from '../../../core/util';

export interface OpcaoDeVenda {
  produto: Produto;
  disponivel: number;
}

export interface SolicitacaoPersonalizadaData {
  opcoes: OpcaoDeVenda[];
}

@Component({
  selector: 'app-solicitacao-personalizada-dialog',
  standalone: false,
  templateUrl: './solicitacao-personalizada-dialog.html',
  styleUrl: './solicitacao-personalizada-dialog.scss',
})
export class SolicitacaoPersonalizadaDialog {
  protected readonly data = inject<SolicitacaoPersonalizadaData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<SolicitacaoPersonalizadaDialog, Requisicao>>(MatDialogRef);
  private readonly api = inject(RequisicaoService);
  private readonly fb = inject(FormBuilder);

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  private disponivelDoProduto: number | null = null;

  protected readonly form = this.fb.group({
    produtoId: this.fb.control<number | null>(null, Validators.required),
    quantidade: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      (c: AbstractControl): ValidationErrors | null => {
        const max = this.disponivelDoProduto;
        return max !== null && c.value !== null && c.value > max ? { max: true } : null;
      },
    ]),
    precoSaca: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    observacao: this.fb.nonNullable.control('', Validators.maxLength(255)),
  });

  private readonly valores = toSignal(this.form.valueChanges, { initialValue: this.form.value });

  protected readonly selecionada = computed(() => {
    const id = this.valores().produtoId;
    return this.data.opcoes.find((o) => o.produto.id === id) ?? null;
  });

  protected readonly total = computed(() => {
    const v = this.valores();
    return v.quantidade && v.precoSaca ? v.quantidade * v.precoSaca : null;
  });

  protected readonly precoDiferente = computed(() => {
    const atual = this.selecionada()?.produto.precoAtual;
    const preco = this.valores().precoSaca;
    return preco != null && atual != null && Math.abs(preco - atual) > 0.0001;
  });

  protected readonly restantes = computed(() => 255 - (this.valores().observacao?.length ?? 0));

  constructor() {
    this.form.controls.produtoId.valueChanges.pipe(takeUntilDestroyed()).subscribe((id) => {
      const opcao = this.data.opcoes.find((o) => o.produto.id === id) ?? null;
      this.disponivelDoProduto = opcao?.disponivel ?? null;
      this.form.controls.precoSaca.setValue(opcao?.produto.precoAtual ?? null);
      this.form.controls.quantidade.updateValueAndValidity();
    });
  }

  protected usarSaldoTotal(): void {
    const opcao = this.selecionada();
    if (opcao) {
      this.form.controls.quantidade.setValue(opcao.disponivel);
    }
  }

  protected enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set(null);
    const preco = this.precoDiferente() ? v.precoSaca : undefined;
    this.api.criar(v.produtoId as number, v.quantidade as number, preco, v.observacao).subscribe({
      next: (r) => this.ref.close(r),
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }
}
