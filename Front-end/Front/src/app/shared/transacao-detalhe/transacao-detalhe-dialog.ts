import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Transacao } from '../../core/models';

export interface TransacaoDetalheData {
  transacao: Transacao;
}

type Aba = 'detalhes' | 'contrato';

const ESTILO_IMPRESSAO = `
  @page { size: A4; margin: 20mm 18mm; }
  * { box-sizing: border-box; }
  body { margin: 0; font-family: Georgia, 'Times New Roman', serif; color: #111; font-size: 12.5pt; line-height: 1.5; }
  .contrato h1 { font-size: 16pt; text-align: center; margin: 0 0 4px; text-transform: uppercase; letter-spacing: .02em; }
  .contrato .numero { text-align: center; margin: 0 0 22px; color: #444; font-size: 11pt; }
  .contrato h2 { font-size: 12.5pt; margin: 18px 0 4px; }
  .contrato p { margin: 0 0 8px; text-align: justify; }
  .contrato ul { margin: 0 0 8px; padding-left: 20px; }
  .contrato .local { margin-top: 26px; text-align: right; }
  .contrato .assinaturas { display: flex; gap: 40px; margin-top: 56px; }
  .contrato .assinatura { flex: 1; text-align: center; border-top: 1px solid #111; padding-top: 6px; font-size: 11pt; }
  .contrato .nota { margin-top: 28px; font-size: 9.5pt; color: #555; text-align: justify; }
`;

@Component({
  selector: 'app-transacao-detalhe-dialog',
  standalone: false,
  templateUrl: './transacao-detalhe-dialog.html',
  styleUrl: './transacao-detalhe-dialog.scss',
})
export class TransacaoDetalheDialog {
  protected readonly data = inject<TransacaoDetalheData>(MAT_DIALOG_DATA);
  private readonly contratoEl = viewChild<ElementRef<HTMLElement>>('contrato');

  protected readonly t = this.data.transacao;
  protected readonly aba = signal<Aba>('detalhes');

  protected readonly numeroContrato = computed(
    () => `SL-${new Date(this.t.criadoEm).getFullYear()}-${String(this.t.id).padStart(6, '0')}`,
  );

  protected readonly compradora = this.t.compradoraNome || 'COMPRADORA (cerealista)';

  protected readonly sedeCerealista = computed(() =>
    this.t.compradoraEndereco ? `, com sede em ${this.t.compradoraEndereco}` : '',
  );

  protected readonly prefixoLocal = computed(() => (this.t.compradoraCidade ? `${this.t.compradoraCidade}, ` : ''));

  protected imprimir(): void {
    const conteudo = this.contratoEl()?.nativeElement.outerHTML;
    if (!conteudo) {
      return;
    }
    const quadro = document.createElement('iframe');
    quadro.setAttribute('aria-hidden', 'true');
    Object.assign(quadro.style, { position: 'fixed', right: '0', bottom: '0', width: '0', height: '0', border: '0' });
    quadro.onload = () => {
      quadro.contentWindow?.focus();
      quadro.contentWindow?.print();
      setTimeout(() => quadro.remove(), 2000);
    };
    quadro.srcdoc =
      `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8">` +
      `<title>Contrato ${this.numeroContrato()}</title><style>${ESTILO_IMPRESSAO}</style></head>` +
      `<body>${conteudo}</body></html>`;
    document.body.appendChild(quadro);
  }
}
