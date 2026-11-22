import { Component, computed, input } from '@angular/core';

export interface PontoPreco {
  data: string;
  valor: number;
}

const LARGURA = 640;
const ALTURA = 260;
const MARGEM = { topo: 16, direita: 16, base: 30, esquerda: 60 };

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dia = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' });

@Component({
  selector: 'app-price-chart',
  standalone: false,
  templateUrl: './price-chart.html',
  styleUrl: './price-chart.scss',
})
export class PriceChart {
  readonly pontos = input.required<PontoPreco[]>();
  readonly descricao = input('Evolução do preço por saca');

  protected readonly largura = LARGURA;
  protected readonly altura = ALTURA;

  protected readonly grafico = computed(() => {
    const pontos = this.pontos();
    if (pontos.length === 0) {
      return null;
    }
    const valores = pontos.map((p) => p.valor);
    let min = Math.min(...valores);
    let max = Math.max(...valores);
    if (min === max) {
      min -= 1;
      max += 1;
    }
    const folga = (max - min) * 0.1;
    min -= folga;
    max += folga;

    const x0 = MARGEM.esquerda;
    const x1 = LARGURA - MARGEM.direita;
    const yBase = ALTURA - MARGEM.base;
    const yTopo = MARGEM.topo;
    const posicoes = pontos.map((p, i) => ({
      x: pontos.length === 1 ? (x0 + x1) / 2 : x0 + ((x1 - x0) * i) / (pontos.length - 1),
      y: yBase - ((yBase - yTopo) * (p.valor - min)) / (max - min),
      texto: `${dia.format(new Date(p.data))}: ${moeda.format(p.valor)}`,
    }));
    const linha = posicoes.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
    const ultimo = posicoes[posicoes.length - 1];
    const area = `${linha} L${ultimo.x.toFixed(1)},${yBase} L${posicoes[0].x.toFixed(1)},${yBase} Z`;
    const grade = [0, 1, 2, 3].map((i) => ({
      y: yBase - ((yBase - yTopo) * i) / 3,
      rotulo: moeda.format(min + ((max - min) * i) / 3),
    }));
    const indices = Array.from(new Set([0, Math.floor((pontos.length - 1) / 2), pontos.length - 1]));
    const eixoX = indices.map((i) => ({ x: posicoes[i].x, rotulo: dia.format(new Date(pontos[i].data)) }));

    return { posicoes, linha, area, grade, eixoX, yBase, x0, x1 };
  });
}
