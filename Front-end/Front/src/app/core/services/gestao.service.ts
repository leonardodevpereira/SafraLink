import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AcaoAuditoria,
  Auditoria,
  DashboardCerealista,
  DashboardProdutor,
  Desconto,
  DescontoRequest,
  Pagina,
  PerfilRequest,
  ProdutorResumo,
  RegistroRequest,
} from '../models';
import { parametros } from '../util';

@Injectable({ providedIn: 'root' })
export class DescontoService {
  private readonly http = inject(HttpClient);

  listar(): Observable<Desconto[]> {
    return this.http.get<Desconto[]>('/api/descontos');
  }

  criar(req: DescontoRequest): Observable<Desconto> {
    return this.http.post<Desconto>('/api/descontos', req);
  }

  atualizar(id: number, req: DescontoRequest): Observable<Desconto> {
    return this.http.put<Desconto>(`/api/descontos/${id}`, req);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/descontos/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class ProdutorService {
  private readonly http = inject(HttpClient);

  listar(busca?: string): Observable<ProdutorResumo[]> {
    return this.http.get<ProdutorResumo[]>('/api/produtores', { params: parametros({ busca }) });
  }

  criar(req: RegistroRequest): Observable<unknown> {
    return this.http.post('/api/produtores', req);
  }

  atualizar(id: number, req: PerfilRequest): Observable<ProdutorResumo> {
    return this.http.put<ProdutorResumo>(`/api/produtores/${id}`, req);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/produtores/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  doProdutor(): Observable<DashboardProdutor> {
    return this.http.get<DashboardProdutor>('/api/dashboard/produtor');
  }

  daCerealista(): Observable<DashboardCerealista> {
    return this.http.get<DashboardCerealista>('/api/dashboard/cerealista');
  }
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private readonly http = inject(HttpClient);

  listar(
    filtros: { acao?: AcaoAuditoria | null; de?: string | null; ate?: string | null; pagina?: number; tamanho?: number },
  ): Observable<Pagina<Auditoria>> {
    return this.http.get<Pagina<Auditoria>>('/api/auditoria', {
      params: parametros({ ...filtros, pagina: filtros.pagina ?? 0, tamanho: filtros.tamanho ?? 20 }),
    });
  }
}

@Injectable({ providedIn: 'root' })
export class RelatorioService {
  private readonly http = inject(HttpClient);

  baixarTransacoes(de?: string | null, ate?: string | null): Observable<Blob> {
    return this.http.get('/api/relatorios/transacoes.csv', { params: parametros({ de, ate }), responseType: 'blob' });
  }

  baixarProdutores(): Observable<Blob> {
    return this.http.get('/api/relatorios/produtores.csv', { responseType: 'blob' });
  }

  salvar(blob: Blob, nomeArquivo: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = nomeArquivo;
    link.click();
    URL.revokeObjectURL(url);
  }
}
