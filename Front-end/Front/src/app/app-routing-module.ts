import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { perfilGuard, visitanteGuard } from './core/guards/auth.guards';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'auth/login' },
  {
    path: 'auth',
    canActivate: [visitanteGuard],
    loadChildren: () => import('./auth/auth-module').then((m) => m.AuthModule),
  },
  {
    path: 'cliente',
    canActivate: [perfilGuard('PRODUTOR')],
    loadChildren: () => import('./cliente/cliente-module').then((m) => m.ClienteModule),
  },
  {
    path: 'cerealista',
    canActivate: [perfilGuard('CEREALISTA')],
    loadChildren: () => import('./cerealista/cerealista-module').then((m) => m.CerealistaModule),
  },
  { path: '**', redirectTo: 'auth/login' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
