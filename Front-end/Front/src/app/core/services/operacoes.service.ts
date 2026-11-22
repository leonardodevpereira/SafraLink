import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Entrega, EntregaRequest, Pagina, Requisicao, Saldo, StatusRequisicao, Transacao } from '../models';
import { parametros } from '../util';

@Injectable({ providedIn: 'root' })
export class SaldoService {
  private readonly http = inject(HttpClient);

  meusSaldos(): Observable<Saldo[]> {
    return this.http.get<Saldo[]>('/api/saldos');
  }

  saldosDoProdutor(produtorId: number): Observable<Saldo[]> {
    return this.http.get<Saldo[]>(`/api/produtores/${produtorId}/saldos`);
  }
}

@Injectable({ providedIn: 'root' })
export class EntregaService {
  private readonly http = inject(HttpClient);

  listar(pagina = 0, tamanho = 20): Observable<Pagina<Entrega>> {
    return this.http.get<Pagina<Entrega>>('/api/entregas', { params: parametros({ pagina, tamanho }) });
  }

  registrar(req: EntregaRequest): Observable<Entrega> {
    return this.http.post<Entrega>('/api/entregas', req);
  }

  atualizar(id: number, req: { quantidade: number; dataEntrega?: string; observacao?: string }): Observable<Entrega> {
    return this.http.put<Entrega>(`/api/entregas/${id}`, req);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/entregas/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class RequisicaoService {
  private readonly http = inject(HttpClient);

  listar(
    filtros: { status?: StatusRequisicao | null; produtorId?: number | null; pagina?: number; tamanho?: number } = {},
  ): Observable<Pagina<Requisicao>> {
    return this.http.get<Pagina<Requisicao>>('/api/requisicoes', {
      params: parametros({
        status: filtros.status,
        produtorId: filtros.produtorId,
        pagina: filtros.pagina ?? 0,
        tamanho: filtros.tamanho ?? 20,
      }),
    });
  }

  buscar(id: number): Observable<Requisicao> {
    return this.http.get<Requisicao>(`/api/requisicoes/${id}`);
  }

  criar(
    produtoId: number,
    quantidade: number,
    precoSaca?: number | null,
    observacao?: string | null,
  ): Observable<Requisicao> {
    return this.http.post<Requisicao>('/api/requisicoes', {
      produtoId,
      quantidade,
      precoSaca: precoSaca ?? undefined,
      observacao: observacao?.trim() || undefined,
    });
  }

  atualizar(
    id: number,
    req: { quantidade: number; precoSaca?: number | null; observacao?: string | null },
  ): Observable<Requisicao> {
    return this.http.put<Requisicao>(`/api/requisicoes/${id}`, {
      quantidade: req.quantidade,
      precoSaca: req.precoSaca ?? undefined,
      observacao: req.observacao?.trim() || undefined,
    });
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/requisicoes/${id}`);
  }

  reenviar(id: number): Observable<Requisicao> {
    return this.http.post<Requisicao>(`/api/requisicoes/${id}/reenviar`, {});
  }

  aprovar(id: number): Observable<Requisicao> {
    return this.http.post<Requisicao>(`/api/requisicoes/${id}/aprovar`, {});
  }

  recusar(id: number, motivo: string): Observable<Requisicao> {
    return this.http.post<Requisicao>(`/api/requisicoes/${id}/recusar`, { motivo });
  }

  processar(id: number): Observable<Requisicao> {
    return this.http.post<Requisicao>(`/api/requisicoes/${id}/processar`, {});
  }

  enviarContraProposta(id: number, precoSaca: number): Observable<Requisicao> {
    return this.http.post<Requisicao>(`/api/requisicoes/${id}/contra-propostas`, { precoSaca });
  }

  aceitarContraProposta(id: number): Observable<Requisicao> {
    return this.http.post<Requisicao>(`/api/contra-propostas/${id}/aceitar`, {});
  }

  recusarContraProposta(id: number): Observable<Requisicao> {
    return this.http.post<Requisicao>(`/api/contra-propostas/${id}/recusar`, {});
  }
}

export interface FiltroTransacoes {
  produtorId?: number | null;
  produtoId?: number | null;
  status?: StatusRequisicao | null;
  de?: string | null;
  ate?: string | null;
  pagina?: number;
  tamanho?: number;
}

@Injectable({ providedIn: 'root' })
export class TransacaoService {
  private readonly http = inject(HttpClient);

  listar(filtros: FiltroTransacoes = {}): Observable<Pagina<Transacao>> {
    return this.http.get<Pagina<Transacao>>('/api/transacoes', {
      params: parametros({ ...filtros, pagina: filtros.pagina ?? 0, tamanho: filtros.tamanho ?? 20 }),
    });
  }
}
