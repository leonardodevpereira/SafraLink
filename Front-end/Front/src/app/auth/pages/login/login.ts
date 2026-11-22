import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Perfil } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { mensagemDeErro } from '../../../core/util';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly perfil = signal<Perfil>('PRODUTOR');
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mostrarSenha = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected escolherPerfil(valor: Perfil): void {
    this.perfil.set(valor);
    this.erro.set(null);
  }

  protected entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    const { email, senha } = this.form.getRawValue();
    this.auth.login({ email: email.trim(), senha, perfil: this.perfil() }).subscribe({
      next: (u) => void this.router.navigateByUrl(this.auth.rotaInicial(u.perfil)),
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível entrar. Tente novamente.'));
        this.enviando.set(false);
      },
    });
  }
}
