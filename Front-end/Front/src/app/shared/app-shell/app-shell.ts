import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, OnDestroy, OnInit, computed, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { NotificacoesService } from '../../core/services/notificacoes.service';

export interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
  exato?: boolean;
  grupo?: string;
}

interface GrupoMenu {
  titulo: string;
  itens: ItemMenu[];
}

@Component({
  selector: 'app-shell',
  standalone: false,
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell implements OnInit, OnDestroy {
  readonly itens = input.required<ItemMenu[]>();
  readonly rotaPerfil = input.required<string>();

  protected readonly auth = inject(AuthService);
  protected readonly notificacoes = inject(NotificacoesService);
  private readonly breakpoints = inject(BreakpointObserver);

  protected readonly mobile = toSignal(
    this.breakpoints.observe('(max-width: 959px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  protected readonly grupos = computed<GrupoMenu[]>(() => {
    const grupos: GrupoMenu[] = [];
    for (const item of this.itens()) {
      const titulo = item.grupo ?? '';
      const ultimo = grupos[grupos.length - 1];
      if (ultimo && ultimo.titulo === titulo) {
        ultimo.itens.push(item);
      } else {
        grupos.push({ titulo, itens: [item] });
      }
    }
    return grupos;
  });

  protected readonly iniciais = computed(() => {
    const partes = (this.auth.usuario()?.nome ?? '').trim().split(/\s+/).filter(Boolean);
    if (partes.length === 0) {
      return '?';
    }
    const primeira = partes[0][0];
    const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
    return (primeira + ultima).toUpperCase();
  });

  protected readonly rotuloPerfil = computed(() => (this.auth.ehCerealista() ? 'Cerealista' : 'Produtor'));

  protected readonly hoje = (() => {
    const texto = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  })();

  ngOnInit(): void {
    this.notificacoes.iniciar();
  }

  ngOnDestroy(): void {
    this.notificacoes.parar();
  }

  protected sair(): void {
    this.auth.logout();
  }
}
