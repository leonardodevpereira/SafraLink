import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }
  const credencial = auth.credencial;
  const cabecalhos: Record<string, string> = { 'X-Requested-With': 'XMLHttpRequest' };
  if (credencial && !req.headers.has('Authorization')) {
    cabecalhos['Authorization'] = `Basic ${credencial}`;
  }
  const requisicao = req.clone({ setHeaders: cabecalhos });

  return next(requisicao).pipe(
    catchError((erro: unknown) => {
      const ehAuth = req.url.startsWith('/api/auth/');
      if (erro instanceof HttpErrorResponse && erro.status === 401 && !ehAuth && credencial && auth.autenticado) {
        auth.logout();
      }
      return throwError(() => erro);
    }),
  );
};
