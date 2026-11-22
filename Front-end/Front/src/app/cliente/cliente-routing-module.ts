import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Perfil } from '../shared/perfil/perfil';
import { ClienteLayout } from './layout/cliente-layout/cliente-layout';
import { Historico } from './pages/historico/historico';
import { Home } from './pages/home/home';
import { Precos } from './pages/precos/precos';
import { Produtos } from './pages/produtos/produtos';
import { Requisicoes } from './pages/requisicoes/requisicoes';
import { Saldos } from './pages/saldos/saldos';

const routes: Routes = [
  {
    path: '',
    component: ClienteLayout,
    children: [
      { path: '', component: Home, title: 'Início | SafraLink' },
      { path: 'saldos', component: Saldos, title: 'Meus saldos | SafraLink' },
      { path: 'produtos', component: Produtos, title: 'Vender | SafraLink' },
      { path: 'requisicoes', component: Requisicoes, title: 'Minhas requisições | SafraLink' },
      { path: 'historico', component: Historico, title: 'Histórico | SafraLink' },
      { path: 'precos', component: Precos, title: 'Preços | SafraLink' },
      { path: 'perfil', component: Perfil, title: 'Meu perfil | SafraLink' },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ClienteRoutingModule {}
