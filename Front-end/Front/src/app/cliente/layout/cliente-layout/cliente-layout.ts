import { Component } from '@angular/core';
import { ItemMenu } from '../../../shared/app-shell/app-shell';

@Component({
  selector: 'app-cliente-layout',
  standalone: false,
  templateUrl: './cliente-layout.html',
  styleUrl: './cliente-layout.scss',
})
export class ClienteLayout {
  protected readonly menu: ItemMenu[] = [
    { rota: '/cliente', rotulo: 'Início', icone: 'home', exato: true },
    { rota: '/cliente/saldos', rotulo: 'Meus saldos', icone: 'inventory_2', grupo: 'Minha safra' },
    { rota: '/cliente/produtos', rotulo: 'Vender', icone: 'sell', grupo: 'Minha safra' },
    { rota: '/cliente/requisicoes', rotulo: 'Minhas requisições', icone: 'receipt_long', grupo: 'Minha safra' },
    { rota: '/cliente/historico', rotulo: 'Histórico', icone: 'history', grupo: 'Consultas' },
    { rota: '/cliente/precos', rotulo: 'Preços', icone: 'show_chart', grupo: 'Consultas' },
  ];
}
