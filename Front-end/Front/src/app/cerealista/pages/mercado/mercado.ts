import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Preco, Produto } from '../../../core/models';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { mensagemDeErro } from '../../../core/util';
import { PontoPreco } from '../../../shared/price-chart/price-chart';

interface Controles {
  preco: FormControl<number | null>;
  quantidade: FormControl<number | null>;
}

@Component({
  selector: 'app-mercado',
  standalone: false,
  templateUrl: './mercado.html',
  styleUrl: './mercado.scss',
})
export class PrecosEQuantidades implements OnInit {
  private readonly api = inject(CatalogoService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly produtos = signal<Produto[]>([]);
  protected readonly controles = signal<Map<number, Controles>>(new Map());
  protected readonly selecionado = signal<Produto | null>(null);
  protected readonly historico = signal<Preco[]>([]);
  protected readonly carregando = signal(true);
  protected readonly salvando = signal<string | null>(null);
  protected readonly erro = signal<string | null>(null);

  protected readonly pontos = computed<PontoPreco[]>(() =>
    this.historico().map((p) => ({ data: p.criadoEm, valor: p.valorSaca })),
  );

  ngOnInit(): void {
    this.carregar(true);
  }

  protected selecionar(produto: Produto): void {
    this.selecionado.set(produto);
    this.api.historicoDePrecos(produto.id, 30).subscribe({
      next: (h) => this.historico.set(h),
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }

  protected atualizarPreco(produto: Produto): void {
    const controle = this.controles().get(produto.id)?.preco;
    if (!controle || controle.invalid || controle.value === null) {
      controle?.markAsTouched();
      return;
    }
    this.salvando.set(`preco-${produto.id}`);
    this.api.definirPreco(produto.id, controle.value).subscribe({
      next: () => {
        this.snackBar.open(`Preço de ${produto.nome} atualizado. Os produtores foram avisados.`, 'OK', { duration: 5000 });
        this.salvando.set(null);
        this.carregar(false, produto.id);
      },
      error: (e: unknown) => this.falhar(e),
    });
  }

  protected atualizarQuantidade(produto: Produto): void {
    const controle = this.controles().get(produto.id)?.quantidade;
    if (!controle || controle.invalid || controle.value === null) {
      controle?.markAsTouched();
      return;
    }
    this.salvando.set(`qtd-${produto.id}`);
    this.api.definirQuantidade(produto.id, controle.value).subscribe({
      next: () => {
        this.snackBar.open(`Quantidade disponível de ${produto.nome} atualizada.`, 'OK', { duration: 4000 });
        this.salvando.set(null);
        this.carregar(false, produto.id);
      },
      error: (e: unknown) => this.falhar(e),
    });
  }

  private carregar(primeira: boolean, manter?: number): void {
    this.erro.set(null);
    this.api.produtos().subscribe({
      next: (produtos) => {
        this.produtos.set(produtos);
        this.controles.set(
          new Map(
            produtos.map((p) => [
              p.id,
              {
                preco: new FormControl<number | null>(p.precoAtual ?? null, [Validators.required, Validators.min(0.01)]),
                quantidade: new FormControl<number | null>(p.quantidadeDisponivel, [Validators.required, Validators.min(0)]),
              },
            ]),
          ),
        );
        this.carregando.set(false);
        const alvo = produtos.find((p) => p.id === (manter ?? this.selecionado()?.id)) ?? (primeira ? produtos[0] : null);
        if (alvo) {
          this.selecionar(alvo);
        }
      },
      error: (e: unknown) => this.falhar(e),
    });
  }

  private falhar(e: unknown): void {
    this.erro.set(mensagemDeErro(e));
    this.carregando.set(false);
    this.salvando.set(null);
  }
}
