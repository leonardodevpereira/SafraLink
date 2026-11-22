import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../shared/shared-module';
import { AuthShell } from './auth-shell/auth-shell';
import { EsqueciSenha } from './pages/esqueci-senha/esqueci-senha';
import { Login } from './pages/login/login';
import { RedefinirSenha } from './pages/redefinir-senha/redefinir-senha';
import { Registro } from './pages/registro/registro';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: Login, title: 'Entrar | SafraLink' },
  { path: 'registro', component: Registro, title: 'Criar conta | SafraLink' },
  { path: 'esqueci-senha', component: EsqueciSenha, title: 'Recuperar senha | SafraLink' },
  { path: 'redefinir-senha', component: RedefinirSenha, title: 'Nova senha | SafraLink' },
];

@NgModule({
  declarations: [AuthShell, Login, Registro, EsqueciSenha, RedefinirSenha],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class AuthModule {}
