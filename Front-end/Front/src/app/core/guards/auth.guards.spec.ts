import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from '../services/auth.service';
import { perfilGuard, visitanteGuard } from './auth.guards';

function executar(guard: ReturnType<typeof perfilGuard>): boolean | UrlTree {
  return TestBed.runInInjectionContext(() =>
    guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
  ) as boolean | UrlTree;
}

describe('guards', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  function entrarComo(perfil: 'PRODUTOR' | 'CEREALISTA'): void {
    auth.login({ email: 'f@teste.dev', senha: 'segredo123' }).subscribe();
    http.expectOne('/api/usuarios/me').flush({
      id: 1,
      nome: 'F',
      email: 'f@teste.dev',
      perfil,
      documento: '1',
      criadoEm: '2026-01-01T00:00:00Z',
    });
  }

  it('manda quem não está logado para o login', () => {
    const resultado = executar(perfilGuard('PRODUTOR'));
    expect(resultado).toBeInstanceOf(UrlTree);
    expect(String(resultado)).toBe('/auth/login');
  });

  it('libera o perfil certo', () => {
    entrarComo('PRODUTOR');
    expect(executar(perfilGuard('PRODUTOR'))).toBe(true);
  });

  it('redireciona o perfil errado para a própria área', () => {
    entrarComo('PRODUTOR');
    expect(String(executar(perfilGuard('CEREALISTA')))).toBe('/cliente');
  });

  it('visitanteGuard leva quem já entrou direto à sua área', () => {
    expect(executar(visitanteGuard as ReturnType<typeof perfilGuard>)).toBe(true);
    entrarComo('CEREALISTA');
    expect(String(executar(visitanteGuard as ReturnType<typeof perfilGuard>))).toBe('/cerealista');
  });
});
