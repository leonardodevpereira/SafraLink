import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Preco, Produto } from '../../../core/models';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { mensagemDeErro } from '../../../core/util';
import { PontoPreco } from '../../../shared/price-chart/price-chart';

@Component({
  selector: 'app-precos',
  standalone: false,
  templateUrl: './precos.html',
  styleUrl: './precos.scss',
})
export class Precos implements OnInit {
  private readonly catalogo = inject(CatalogoService);

  protected readonly produtos = signal<Produto[]>([]);
  protected readonly produtoId = signal<number | null>(null);
  protected readonly dias = signal(30);
  protected readonly historico = signal<Preco[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly pontos = computed<PontoPreco[]>(() =>
    this.historico().map((p) => ({ data: p.criadoEm, valor: p.valorSaca })),
  );

  protected readonly resumo = computed(() => {
    const valores = this.historico().map((p) => p.valorSaca);
    if (valores.length === 0) {
      return null;
    }
    const atual = valores[valores.length - 1];
    const primeiro = valores[0];
    return {
      atual,
      minimo: Math.min(...valores),
      maximo: Math.max(...valores),
      variacao: primeiro > 0 ? ((atual - primeiro) / primeiro) * 100 : 0,
    };
  });

  protected readonly linhas = computed(() => {
    const lista = this.historico();
    return lista
      .map((p, i) => ({
        ...p,
        variacao: i > 0 && lista[i - 1].valorSaca > 0 ? ((p.valorSaca - lista[i - 1].valorSaca) / lista[i - 1].valorSaca) * 100 : null,
      }))
      .reverse();
  });

  ngOnInit(): void {
    this.catalogo.produtos().subscribe({
      next: (produtos) => {
        this.produtos.set(produtos);
        if (produtos.length > 0) {
          this.selecionar(produtos[0].id);
        } else {
          this.carregando.set(false);
        }
      },
      error: (e: unknown) => this.falhar(e),
    });
  }

  protected selecionar(id: number): void {
    this.produtoId.set(id);
    this.carregar();
  }

  protected mudarPeriodo(dias: number): void {
    this.dias.set(dias);
    this.carregar();
  }

  private carregar(): void {
    const id = this.produtoId();
    if (id === null) {
      return;
    }
    this.carregando.set(true);
    this.erro.set(null);
    this.catalogo.historicoDePrecos(id, this.dias()).subscribe({
      next: (lista) => {
        this.historico.set(lista);
        this.carregando.set(false);
      },
      error: (e: unknown) => this.falhar(e),
    });
  }

  private falhar(e: unknown): void {
    this.erro.set(mensagemDeErro(e, 'Não foi possível carregar o histórico de preços.'));
    this.carregando.set(false);
  }
}
