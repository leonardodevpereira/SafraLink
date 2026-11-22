import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { ContraProposta, Requisicao } from '../../core/models';
import { RequisicaoService } from '../../core/services/operacoes.service';
import { AuthService } from '../../core/services/auth.service';
import { mensagemDeErro } from '../../core/util';
import { ConfirmDialog, PromptDialog } from '../dialogs/dialogs';

export interface RequisicaoDetalheData {
  requisicao: Requisicao;
}

@Component({
  selector: 'app-requisicao-detalhe',
  standalone: false,
  templateUrl: './requisicao-detalhe.html',
  styleUrl: './requisicao-detalhe.scss',
})
export class RequisicaoDetalheDialog {
  private readonly data = inject<RequisicaoDetalheData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<RequisicaoDetalheDialog, boolean>>(MatDialogRef);
  private readonly api = inject(RequisicaoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly auth = inject(AuthService);

  protected readonly requisicao = signal<Requisicao>(this.data.requisicao);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly ehCerealista = this.auth.ehCerealista;

  private alterou = false;

  protected readonly propostaPendente = computed(
    () => this.requisicao().contraPropostas.find((c) => c.status === 'PENDENTE') ?? null,
  );
  protected readonly podeNegociar = computed(() => this.requisicao().status === 'PENDENTE');

  protected readonly preco = new FormControl<number | null>(null, [Validators.required, Validators.min(0.01)]);

  protected souAutor(proposta: ContraProposta): boolean {
    return proposta.autorId === this.auth.usuario()?.id;
  }

  protected fechar(): void {
    this.ref.close(this.alterou);
  }

  protected aprovar(): void {
    const r = this.requisicao();
    const total = (r.valorBruto ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    this.confirmar(
      'Aprovar venda',
      `Aprovar a compra de ${r.quantidade} sc de ${r.produtoNome} de ${r.produtorNome} por ${total}? O saldo do produtor será debitado e os descontos aplicados.`,
      'Aprovar',
      () => this.executar(this.api.aprovar(r.id), 'Venda aprovada.'),
    );
  }

  protected recusar(): void {
    const r = this.requisicao();
    this.dialog
      .open<PromptDialog, { titulo: string; rotulo: string; confirmar: string }, string>(PromptDialog, {
        data: { titulo: 'Recusar venda', rotulo: 'Motivo da recusa', confirmar: 'Recusar' },
        width: '420px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((motivo) => {
        if (motivo) {
          this.executar(this.api.recusar(r.id, motivo), 'Venda recusada.');
        }
      });
  }

  protected processar(): void {
    const r = this.requisicao();
    this.confirmar(
      'Marcar como processada',
      'Confirma que a entrega e o pagamento desta venda foram concluídos?',
      'Confirmar',
      () => this.executar(this.api.processar(r.id), 'Venda marcada como processada.'),
    );
  }

  protected reenviar(): void {
    this.executar(this.api.reenviar(this.requisicao().id), 'Requisição reenviada para análise.');
  }

  protected enviarProposta(): void {
    this.preco.markAsTouched();
    if (this.preco.invalid || this.preco.value === null) {
      return;
    }
    this.executar(this.api.enviarContraProposta(this.requisicao().id, this.preco.value), 'Contra-proposta enviada.', () =>
      this.preco.reset(),
    );
  }

  protected aceitarProposta(proposta: ContraProposta): void {
    this.executar(this.api.aceitarContraProposta(proposta.id), 'Contra-proposta aceita. O preço foi atualizado.');
  }

  protected recusarProposta(proposta: ContraProposta): void {
    this.executar(this.api.recusarContraProposta(proposta.id), 'Contra-proposta recusada.');
  }

  private confirmar(titulo: string, mensagem: string, confirmar: string, aoConfirmar: () => void): void {
    this.dialog
      .open<ConfirmDialog, { titulo: string; mensagem: string; confirmar: string }, boolean>(ConfirmDialog, {
        data: { titulo, mensagem, confirmar },
        width: '440px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          aoConfirmar();
        }
      });
  }

  private executar(chamada: Observable<Requisicao>, sucesso: string, aposSucesso?: () => void): void {
    this.ocupado.set(true);
    this.erro.set(null);
    chamada.subscribe({
      next: (atualizada) => {
        this.requisicao.set(atualizada);
        this.alterou = true;
        this.ocupado.set(false);
        this.snackBar.open(sucesso, 'OK', { duration: 4000 });
        aposSucesso?.();
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.ocupado.set(false);
      },
    });
  }
}
