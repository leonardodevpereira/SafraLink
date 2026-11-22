import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { mensagemDeErro, senhasIguais, somenteDigitos, validarCpf } from '../../../core/util';

@Component({
  selector: 'app-registro',
  standalone: false,
  templateUrl: './registro.html',
  styleUrl: './registro.scss',
})
export class Registro {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group(
    {
      nome: ['', [Validators.required, Validators.maxLength(150)]],
      email: ['', [Validators.required, Validators.email]],
      cpf: ['', [Validators.required, validarCpf]],
      telefone: [''],
      cidade: [''],
      uf: ['', [Validators.maxLength(2), Validators.pattern(/^([A-Za-z]{2})?$/)]],
      senha: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmacao: ['', Validators.required],
    },
    { validators: senhasIguais('senha', 'confirmacao') },
  );

  protected cadastrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set(null);
    this.auth
      .registrar({
        nome: v.nome.trim(),
        email: v.email.trim(),
        senha: v.senha,
        cpf: somenteDigitos(v.cpf),
        telefone: v.telefone.trim() || undefined,
        endereco: { cidade: v.cidade.trim() || undefined, uf: v.uf.trim() || undefined },
      })
      .subscribe({
        next: () => void this.router.navigateByUrl(this.auth.rotaInicial()),
        error: (e: unknown) => {
          this.erro.set(mensagemDeErro(e));
          this.enviando.set(false);
        },
      });
  }
}
