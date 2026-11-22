import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Mercado, Preco, Produto, Quantidade, TipoGrao } from '../models';
import { parametros } from '../util';

export interface ProdutoRequest {
  nome: string;
  grao: TipoGrao;
  mercado: Mercado;
}

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);

  produtos(): Observable<Produto[]> {
    return this.http.get<Produto[]>('/api/produtos');
  }

  criarProduto(req: ProdutoRequest): Observable<Produto> {
    return this.http.post<Produto>('/api/produtos', req);
  }

  atualizarProduto(id: number, req: ProdutoRequest): Observable<Produto> {
    return this.http.put<Produto>(`/api/produtos/${id}`, req);
  }

  excluirProduto(id: number): Observable<void> {
    return this.http.delete<void>(`/api/produtos/${id}`);
  }

  historicoDePrecos(produtoId: number, dias = 30): Observable<Preco[]> {
    return this.http.get<Preco[]>('/api/precos/historico', { params: parametros({ produtoId, dias }) });
  }

  definirPreco(produtoId: number, valorSaca: number): Observable<Preco> {
    return this.http.put<Preco>(`/api/precos/${produtoId}`, { valorSaca });
  }

  quantidades(): Observable<Quantidade[]> {
    return this.http.get<Quantidade[]>('/api/quantidades');
  }

  definirQuantidade(produtoId: number, quantidade: number): Observable<Quantidade> {
    return this.http.put<Quantidade>(`/api/quantidades/${produtoId}`, { quantidade });
  }
}
