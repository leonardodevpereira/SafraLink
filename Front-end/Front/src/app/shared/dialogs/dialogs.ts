import { Component, inject } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface ConfirmacaoData {
  titulo: string;
  mensagem: string;
  confirmar?: string;
  cancelar?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>{{ data.titulo }}</h2>
    <mat-dialog-content>
      <p>{{ data.mensagem }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">{{ data.cancelar ?? 'Cancelar' }}</button>
      <button mat-flat-button [mat-dialog-close]="true" cdkFocusInitial>{{ data.confirmar ?? 'Confirmar' }}</button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmacaoData>(MAT_DIALOG_DATA);
}

export interface PerguntaData {
  titulo: string;
  rotulo: string;
  confirmar?: string;
  maxLength?: number;
}

@Component({
  selector: 'app-prompt-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>{{ data.titulo }}</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full">
        <mat-label>{{ data.rotulo }}</mat-label>
        <textarea matInput rows="3" [formControl]="texto" [maxlength]="data.maxLength ?? 255" cdkFocusInitial></textarea>
        @if (texto.hasError('required') && texto.touched) {
          <mat-error>Este campo é obrigatório.</mat-error>
        }
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="ref.close()">Cancelar</button>
      <button mat-flat-button (click)="confirmar()">{{ data.confirmar ?? 'Confirmar' }}</button>
    </mat-dialog-actions>
  `,
})
export class PromptDialog {
  protected readonly data = inject<PerguntaData>(MAT_DIALOG_DATA);
  protected readonly ref = inject<MatDialogRef<PromptDialog, string>>(MatDialogRef);
  protected readonly texto = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  protected confirmar(): void {
    this.texto.markAsTouched();
    const valor = this.texto.value.trim();
    if (valor) {
      this.ref.close(valor);
    }
  }
}
