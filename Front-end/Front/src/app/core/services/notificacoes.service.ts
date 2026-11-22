import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription, catchError, of, switchMap, timer } from 'rxjs';
import { Notificacao } from '../models';

const INTERVALO_MS = 20_000;

@Injectable({ providedIn: 'root' })
export class NotificacoesService {
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);

  readonly recentes = signal<Notificacao[]>([]);
  readonly naoLidas = computed(() => this.recentes().filter((n) => !n.lida).length);

  private assinatura: Subscription | null = null;
  private conhecidas = new Set<number>();
  private primeiraCarga = true;

  iniciar(): void {
    this.parar();
    this.assinatura = timer(0, INTERVALO_MS)
      .pipe(
        switchMap(() => this.http.get<Notificacao[]>('/api/notificacoes').pipe(catchError(() => of(null)))),
      )
      .subscribe((lista) => {
        if (lista) {
          this.processar(lista);
        }
      });
  }

  parar(): void {
    this.assinatura?.unsubscribe();
    this.assinatura = null;
    this.recentes.set([]);
    this.conhecidas = new Set<number>();
    this.primeiraCarga = true;
  }

  marcarTodasComoLidas(): void {
    this.http.post<void>('/api/notificacoes/lidas', {}).subscribe(() => {
      this.recentes.update((lista) => lista.map((n) => ({ ...n, lida: true })));
    });
  }

  limpar(): void {
    this.http.delete<void>('/api/notificacoes').subscribe(() => this.recentes.set([]));
  }

  marcarComoLida(id: number): void {
    this.http.post<void>(`/api/notificacoes/${id}/lida`, {}).subscribe(() => {
      this.recentes.update((lista) => lista.map((n) => (n.id === id ? { ...n, lida: true } : n)));
    });
  }

  private processar(lista: Notificacao[]): void {
    const novas = lista.filter((n) => !n.lida && !this.conhecidas.has(n.id));
    lista.forEach((n) => this.conhecidas.add(n.id));
    this.recentes.set(lista);

    if (this.primeiraCarga) {
      this.primeiraCarga = false;
      if (novas.length > 0) {
        this.avisar(novas.length === 1 ? novas[0].mensagem : `Você tem ${novas.length} notificações não lidas.`);
      }
      return;
    }
    if (novas.length > 0) {
      const ultima = novas[0];
      this.avisar(novas.length === 1 ? ultima.mensagem : `${ultima.mensagem} (+${novas.length - 1})`);
    }
  }

  private avisar(mensagem: string): void {
    this.snackBar.open(mensagem, 'OK', { duration: 6000, horizontalPosition: 'end', verticalPosition: 'bottom' });
  }
}
