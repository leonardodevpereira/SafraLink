import { Component } from '@angular/core';
import { ItemMenu } from '../../shared/app-shell/app-shell';

@Component({
  selector: 'app-cerealista-layout',
  standalone: false,
  template: `
    <app-shell [itens]="menu" rotaPerfil="/cerealista/perfil">
      <router-outlet />
    </app-shell>
  `,
})
export class CerealistaLayout {
  protected readonly menu: ItemMenu[] = [
    { rota: '/cerealista', rotulo: 'Painel', icone: 'dashboard', exato: true },
    { rota: '/cerealista/requisicoes', rotulo: 'Requisições', icone: 'fact_check', grupo: 'Operação' },
    { rota: '/cerealista/entregas', rotulo: 'Entregas', icone: 'local_shipping', grupo: 'Operação' },
    { rota: '/cerealista/mercado', rotulo: 'Preços e quantidades', icone: 'price_change', grupo: 'Operação' },
    { rota: '/cerealista/produtores', rotulo: 'Produtores', icone: 'groups', grupo: 'Cadastros' },
    { rota: '/cerealista/produtos', rotulo: 'Produtos', icone: 'inventory_2', grupo: 'Cadastros' },
    { rota: '/cerealista/descontos', rotulo: 'Descontos', icone: 'percent', grupo: 'Cadastros' },
    { rota: '/cerealista/transacoes', rotulo: 'Transações', icone: 'receipt_long', grupo: 'Controle' },
    { rota: '/cerealista/auditoria', rotulo: 'Auditoria', icone: 'shield', grupo: 'Controle' },
  ];
}
