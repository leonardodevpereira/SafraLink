import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { AcaoAuditoria, Auditoria } from '../../../core/models';
import { AuditoriaService } from '../../../core/services/gestao.service';
import { mensagemDeErro } from '../../../core/util';

const ROTULOS: Record<AcaoAuditoria, string> = {
  LOGIN: 'Login',
  LOGIN_FALHA: 'Falha de login',
  CADASTRO: 'Cadastro de usuário',
  PERFIL_ATUALIZADO: 'Perfil atualizado',
  SENHA_REDEFINIDA: 'Senha redefinida',
  ALTERACAO_PRECO: 'Alteração de preço',
  ALTERACAO_QUANTIDADE: 'Alteração de quantidade',
  ENTREGA_REGISTRADA: 'Entrega registrada',
  REQUISICAO_CRIADA: 'Requisição criada',
  REQUISICAO_REENVIADA: 'Requisição reenviada',
  CONTRA_PROPOSTA_ENVIADA: 'Contra-proposta enviada',
  CONTRA_PROPOSTA_ACEITA: 'Contra-proposta aceita',
  CONTRA_PROPOSTA_RECUSADA: 'Contra-proposta recusada',
  APROVACAO: 'Venda aprovada',
  RECUSA: 'Venda recusada',
  PROCESSAMENTO: 'Venda processada',
  DESCONTO_CRIADO: 'Desconto criado',
  DESCONTO_ALTERADO: 'Desconto alterado',
  DESCONTO_EXCLUIDO: 'Desconto excluído',
  PRODUTO_CRIADO: 'Produto criado',
  PRODUTO_ALTERADO: 'Produto alterado',
  PRODUTO_EXCLUIDO: 'Produto excluído',
  PRECO_EXCLUIDO: 'Preço excluído',
  ENTREGA_ALTERADA: 'Entrega alterada',
  ENTREGA_EXCLUIDA: 'Entrega excluída',
  REQUISICAO_ALTERADA: 'Requisição alterada',
  REQUISICAO_EXCLUIDA: 'Requisição excluída',
  PRODUTOR_CADASTRADO: 'Produtor cadastrado',
  PRODUTOR_INATIVADO: 'Produtor inativado',
};

@Component({
  selector: 'app-auditoria',
  standalone: false,
  templateUrl: './auditoria.html',
  styleUrl: './auditoria.scss',
})
export class AuditoriaPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AuditoriaService);

  protected readonly acoes = (Object.keys(ROTULOS) as AcaoAuditoria[]).map((valor) => ({ valor, rotulo: ROTULOS[valor] }));
  protected readonly itens = signal<Auditoria[]>([]);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(20);
  protected readonly total = signal(0);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly filtros = this.fb.group({
    acao: this.fb.control<AcaoAuditoria | null>(null),
    de: this.fb.nonNullable.control(''),
    ate: this.fb.nonNullable.control(''),
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected rotulo(acao: AcaoAuditoria): string {
    return ROTULOS[acao] ?? acao;
  }

  protected aplicar(): void {
    this.pagina.set(0);
    this.carregar();
  }

  protected limpar(): void {
    this.filtros.reset({ de: '', ate: '' });
    this.aplicar();
  }

  protected mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  private carregar(): void {
    const v = this.filtros.getRawValue();
    this.carregando.set(true);
    this.erro.set(null);
    this.api
      .listar({ acao: v.acao, de: v.de || null, ate: v.ate || null, pagina: this.pagina(), tamanho: this.tamanho() })
      .subscribe({
        next: (p) => {
          this.itens.set(p.conteudo);
          this.total.set(p.total);
          this.carregando.set(false);
        },
        error: (e: unknown) => {
          this.erro.set(mensagemDeErro(e, 'Não foi possível carregar a auditoria.'));
          this.carregando.set(false);
        },
      });
  }
}
