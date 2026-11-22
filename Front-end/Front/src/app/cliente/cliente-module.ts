import { NgModule } from '@angular/core';

import { SharedModule } from '../shared/shared-module';
import { ClienteRoutingModule } from './cliente-routing-module';
import { ClienteLayout } from './layout/cliente-layout/cliente-layout';
import { Historico } from './pages/historico/historico';
import { Home } from './pages/home/home';
import { Precos } from './pages/precos/precos';
import { Produtos } from './pages/produtos/produtos';
import { SolicitacaoPersonalizadaDialog } from './pages/produtos/solicitacao-personalizada-dialog';
import { SolicitarVendaDialog } from './pages/produtos/solicitar-venda-dialog';
import { RequisicaoEdicaoDialog } from './pages/requisicoes/requisicao-edicao-dialog';
import { Requisicoes } from './pages/requisicoes/requisicoes';
import { Saldos } from './pages/saldos/saldos';

@NgModule({
  declarations: [
    ClienteLayout,
    Home,
    Saldos,
    Produtos,
    SolicitarVendaDialog,
    SolicitacaoPersonalizadaDialog,
    Requisicoes,
    RequisicaoEdicaoDialog,
    Historico,
    Precos,
  ],
  imports: [SharedModule, ClienteRoutingModule],
})
export class ClienteModule {}
