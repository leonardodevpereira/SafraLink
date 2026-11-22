import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Perfil } from '../shared/perfil/perfil';
import { SharedModule } from '../shared/shared-module';
import { CerealistaLayout } from './layout/cerealista-layout';
import { AuditoriaPage } from './pages/auditoria/auditoria';
import { Dashboard } from './pages/dashboard/dashboard';
import { DescontoDialog } from './pages/descontos/desconto-dialog';
import { Descontos } from './pages/descontos/descontos';
import { EntregaDialog } from './pages/entregas/entrega-dialog';
import { Entregas } from './pages/entregas/entregas';
import { PrecosEQuantidades } from './pages/mercado/mercado';
import { ProdutoDialog } from './pages/produtos/produto-dialog';
import { CadastroProdutos } from './pages/produtos/produtos';
import { Produtores } from './pages/produtores/produtores';
import { ProdutorDialog } from './pages/produtores/produtor-dialog';
import { SaldosProdutorDialog } from './pages/produtores/saldos-produtor-dialog';
import { FilaRequisicoes } from './pages/requisicoes/fila-requisicoes';
import { Transacoes } from './pages/transacoes/transacoes';

const routes: Routes = [
  {
    path: '',
    component: CerealistaLayout,
    children: [
      { path: '', component: Dashboard, title: 'Painel | SafraLink' },
      { path: 'requisicoes', component: FilaRequisicoes, title: 'Requisições | SafraLink' },
      { path: 'mercado', component: PrecosEQuantidades, title: 'Preços e quantidades | SafraLink' },
      { path: 'produtos', component: CadastroProdutos, title: 'Produtos | SafraLink' },
      { path: 'produtores', component: Produtores, title: 'Produtores | SafraLink' },
      { path: 'entregas', component: Entregas, title: 'Entregas | SafraLink' },
      { path: 'transacoes', component: Transacoes, title: 'Transações | SafraLink' },
      { path: 'descontos', component: Descontos, title: 'Descontos | SafraLink' },
      { path: 'auditoria', component: AuditoriaPage, title: 'Auditoria | SafraLink' },
      { path: 'perfil', component: Perfil, title: 'Meu perfil | SafraLink' },
    ],
  },
];

@NgModule({
  declarations: [
    CerealistaLayout,
    Dashboard,
    FilaRequisicoes,
    PrecosEQuantidades,
    CadastroProdutos,
    ProdutoDialog,
    EntregaDialog,
    Produtores,
    ProdutorDialog,
    SaldosProdutorDialog,
    Entregas,
    Transacoes,
    Descontos,
    DescontoDialog,
    AuditoriaPage,
  ],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class CerealistaModule {}
