import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Usuario } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { mensagemDeErro } from '../../core/util';

@Component({
  selector: 'app-perfil',
  standalone: false,
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
})
export class Perfil implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly usuario = signal<Usuario | null>(null);
  protected readonly carregando = signal(true);
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    telefone: ['', Validators.maxLength(20)],
    cep: ['', Validators.maxLength(9)],
    logradouro: ['', Validators.maxLength(150)],
    numero: ['', Validators.maxLength(20)],
    bairro: ['', Validators.maxLength(100)],
    cidade: ['', Validators.maxLength(100)],
    uf: ['', [Validators.maxLength(2), Validators.pattern(/^([A-Za-z]{2})?$/)]],
  });

  ngOnInit(): void {
    this.auth.obterPerfil().subscribe({
      next: (u) => {
        this.usuario.set(u);
        this.form.patchValue({
          nome: u.nome,
          email: u.email,
          telefone: u.telefone ?? '',
          cep: u.endereco?.cep ?? '',
          logradouro: u.endereco?.logradouro ?? '',
          numero: u.endereco?.numero ?? '',
          bairro: u.endereco?.bairro ?? '',
          cidade: u.endereco?.cidade ?? '',
          uf: u.endereco?.uf ?? '',
        });
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar seu perfil.'));
        this.carregando.set(false);
      },
    });
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.erro.set(null);
    this.auth
      .atualizarPerfil({
        nome: v.nome,
        email: v.email,
        telefone: v.telefone,
        endereco: {
          cep: v.cep,
          logradouro: v.logradouro,
          numero: v.numero,
          bairro: v.bairro,
          cidade: v.cidade,
          uf: v.uf,
        },
      })
      .subscribe({
        next: (u) => {
          this.usuario.set(u);
          this.salvando.set(false);
          this.snackBar.open('Perfil atualizado.', 'OK', { duration: 4000 });
        },
        error: (e: unknown) => {
          this.erro.set(mensagemDeErro(e));
          this.salvando.set(false);
        },
      });
  }
}
