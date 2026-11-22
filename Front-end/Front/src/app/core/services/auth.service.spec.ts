import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Usuario } from '../models';
import { AuthService, credencialBasic } from './auth.service';

function usuario(perfil: 'PRODUTOR' | 'CEREALISTA'): Usuario {
  return { id: 1, nome: 'Fulano', email: 'f@teste.dev', perfil, documento: '52998224725', criadoEm: '2026-01-01T00:00:00Z' };
}

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('começa sem sessão', () => {
    expect(auth.autenticado).toBe(false);
    expect(auth.credencial).toBeNull();
    expect(auth.perfil()).toBeNull();
  });

  it('monta a credencial Basic em Base64 com UTF-8', () => {
    expect(credencialBasic('a@b.c', 'senha')).toBe(btoa('a@b.c:senha'));
    expect(atob(credencialBasic('a@b.c', 'ação'))).not.toBe('');
  });

  it('login valida a credencial em /api/usuarios/me e guarda a sessão', () => {
    auth.login({ email: 'f@teste.dev', senha: 'segredo123', perfil: 'PRODUTOR' }).subscribe();
    const req = http.expectOne('/api/usuarios/me');
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('Authorization')).toBe(`Basic ${btoa('f@teste.dev:segredo123')}`);
    req.flush(usuario('PRODUTOR'));

    expect(auth.autenticado).toBe(true);
    expect(auth.credencial).toBe(btoa('f@teste.dev:segredo123'));
    expect(auth.perfil()).toBe('PRODUTOR');
    expect(auth.ehCerealista()).toBe(false);
    expect(auth.rotaInicial()).toBe('/cliente');
  });

  it('recusa quando o perfil escolhido não bate com a conta', () => {
    let falhou = false;
    auth.login({ email: 'f@teste.dev', senha: 'segredo123', perfil: 'CEREALISTA' }).subscribe({ error: () => (falhou = true) });
    http.expectOne('/api/usuarios/me').flush(usuario('PRODUTOR'));
    expect(falhou).toBe(true);
    expect(auth.autenticado).toBe(false);
  });

  it('rotaInicial separa as áreas de cada perfil', () => {
    expect(auth.rotaInicial('CEREALISTA')).toBe('/cerealista');
    expect(auth.rotaInicial('PRODUTOR')).toBe('/cliente');
    expect(auth.rotaInicial(null)).toBe('/auth/login');
  });

  it('logout limpa a sessão', () => {
    auth.login({ email: 'f@teste.dev', senha: 'segredo123' }).subscribe();
    http.expectOne('/api/usuarios/me').flush(usuario('CEREALISTA'));
    expect(auth.ehCerealista()).toBe(true);

    auth.logout(false);
    expect(auth.autenticado).toBe(false);
    expect(auth.usuario()).toBeNull();
    expect(sessionStorage.getItem('safralink.sessao')).toBeNull();
  });
});
