import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { LoginRequest, Perfil, PerfilRequest, RegistroRequest, Usuario } from '../models';

const CHAVE_SESSAO = 'safralink.sessao';

interface Sessao {
  credencial: string;
  usuario: Usuario;
}

export function credencialBasic(email: string, senha: string): string {
  const bytes = new TextEncoder().encode(`${email}:${senha}`);
  let binario = '';
  bytes.forEach((b) => (binario += String.fromCharCode(b)));
  return btoa(binario);
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sessao = signal<Sessao | null>(this.lerSessao());

  readonly usuario = computed(() => this.sessao()?.usuario ?? null);
  readonly perfil = computed<Perfil | null>(() => this.usuario()?.perfil ?? null);
  readonly ehCerealista = computed(() => this.perfil() === 'CEREALISTA');

  get credencial(): string | null {
    return this.sessao()?.credencial ?? null;
  }

  get autenticado(): boolean {
    return this.sessao() !== null;
  }

  login(req: LoginRequest): Observable<Usuario> {
    const credencial = credencialBasic(req.email, req.senha);
    const cabecalhos = new HttpHeaders({ Authorization: `Basic ${credencial}` });
    return this.http.get<Usuario>('/api/usuarios/me', { headers: cabecalhos }).pipe(
      map((usuario) => {
        if (req.perfil && usuario.perfil !== req.perfil) {
          throw { error: { mensagem: 'E-mail, senha ou perfil inválidos.' } };
        }
        return usuario;
      }),
      tap((usuario) => this.iniciar({ credencial, usuario })),
    );
  }

  registrar(req: RegistroRequest): Observable<Usuario> {
    return this.http
      .post<Usuario>('/api/auth/registro', req)
      .pipe(tap((usuario) => this.iniciar({ credencial: credencialBasic(req.email, req.senha), usuario })));
  }

  esqueciSenha(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/esqueci-senha', { email });
  }

  redefinirSenha(token: string, novaSenha: string): Observable<void> {
    return this.http.post<void>('/api/auth/redefinir-senha', { token, novaSenha });
  }

  obterPerfil(): Observable<Usuario> {
    return this.http.get<Usuario>('/api/usuarios/me');
  }

  atualizarPerfil(req: PerfilRequest, senhaAtual?: string): Observable<Usuario> {
    return this.http.put<Usuario>('/api/usuarios/me', req).pipe(tap((u) => this.atualizarUsuario(u, senhaAtual)));
  }

  logout(irParaLogin = true): void {
    this.sessao.set(null);
    this.armazenar(null);
    if (irParaLogin) {
      void this.router.navigateByUrl('/auth/login');
    }
  }

  rotaInicial(perfil: Perfil | null = this.perfil()): string {
    return perfil === 'CEREALISTA' ? '/cerealista' : perfil === 'PRODUTOR' ? '/cliente' : '/auth/login';
  }

  private iniciar(sessao: Sessao): void {
    this.sessao.set(sessao);
    this.armazenar(sessao);
  }

  private atualizarUsuario(usuario: Usuario, senhaAtual?: string): void {
    const atual = this.sessao();
    if (!atual) {
      return;
    }
    let credencial = atual.credencial;
    if (usuario.email !== atual.usuario.email) {
      if (senhaAtual) {
        credencial = credencialBasic(usuario.email, senhaAtual);
      } else {
        this.logout();
        return;
      }
    }
    this.iniciar({ credencial, usuario });
  }

  private lerSessao(): Sessao | null {
    try {
      const bruto = sessionStorage.getItem(CHAVE_SESSAO);
      return bruto ? (JSON.parse(bruto) as Sessao) : null;
    } catch {
      return null;
    }
  }

  private armazenar(sessao: Sessao | null): void {
    try {
      if (sessao) {
        sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
      } else {
        sessionStorage.removeItem(CHAVE_SESSAO);
      }
    } catch {
    }
  }
}
