import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Perfil } from '../models';
import { AuthService } from '../services/auth.service';

export const perfilGuard =
  (perfil: Perfil): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.autenticado) {
      return router.parseUrl('/auth/login');
    }
    return auth.perfil() === perfil ? true : router.parseUrl(auth.rotaInicial());
  };

export const visitanteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.autenticado ? router.parseUrl(auth.rotaInicial()) : true;
};
