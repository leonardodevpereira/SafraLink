export type Perfil = 'PRODUTOR' | 'CEREALISTA';
export type StatusRequisicao = 'PENDENTE' | 'APROVADA' | 'RECUSADA' | 'PROCESSADA';
export type StatusContraProposta = 'PENDENTE' | 'ACEITA' | 'RECUSADA' | 'CANCELADA';
export type CategoriaDesconto = 'ARMAZENAGEM' | 'TRANSPORTE' | 'OUTRO';
export type FormaDesconto = 'PERCENTUAL' | 'VALOR_FIXO';
export type Mercado = 'INTERNO' | 'EXTERNO';
export type TipoGrao = 'SOJA' | 'MILHO' | 'FEIJAO';

export type AcaoAuditoria =
  | 'LOGIN'
  | 'LOGIN_FALHA'
  | 'CADASTRO'
  | 'PERFIL_ATUALIZADO'
  | 'SENHA_REDEFINIDA'
  | 'ALTERACAO_PRECO'
  | 'ALTERACAO_QUANTIDADE'
  | 'ENTREGA_REGISTRADA'
  | 'REQUISICAO_CRIADA'
  | 'REQUISICAO_REENVIADA'
  | 'CONTRA_PROPOSTA_ENVIADA'
  | 'CONTRA_PROPOSTA_ACEITA'
  | 'CONTRA_PROPOSTA_RECUSADA'
  | 'APROVACAO'
  | 'RECUSA'
  | 'PROCESSAMENTO'
  | 'DESCONTO_CRIADO'
  | 'DESCONTO_ALTERADO'
  | 'DESCONTO_EXCLUIDO'
  | 'PRODUTO_CRIADO'
  | 'PRODUTO_ALTERADO'
  | 'PRODUTO_EXCLUIDO'
  | 'PRECO_EXCLUIDO'
  | 'ENTREGA_ALTERADA'
  | 'ENTREGA_EXCLUIDA'
  | 'REQUISICAO_ALTERADA'
  | 'REQUISICAO_EXCLUIDA'
  | 'PRODUTOR_CADASTRADO'
  | 'PRODUTOR_INATIVADO'
  | 'CONTRA_PROPOSTA_ALTERADA'
  | 'CONTRA_PROPOSTA_EXCLUIDA';

export interface Endereco {
  logradouro?: string | null;
  numero?: string | null;
  bairro?: string | null;
  cidade?: string | null;
  uf?: string | null;
  cep?: string | null;
}

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  telefone?: string | null;
  perfil: Perfil;
  documento: string;
  razaoSocial?: string | null;
  endereco?: Endereco | null;
  criadoEm: string;
}

export interface LoginRequest {
  email: string;
  senha: string;
  perfil?: Perfil;
}

export interface RegistroRequest {
  nome: string;
  email: string;
  senha: string;
  cpf: string;
  telefone?: string;
  endereco?: Endereco;
}

export interface PerfilRequest {
  nome: string;
  email: string;
  telefone?: string;
  endereco?: Endereco;
}

export interface Produto {
  id: number;
  nome: string;
  grao: TipoGrao;
  mercado: Mercado;
  precoAtual?: number | null;
  variacaoPercentual?: number | null;
  precoAtualizadoEm?: string | null;
  quantidadeDisponivel: number;
}

export interface Preco {
  id: number;
  produtoId: number;
  produtoNome: string;
  valorSaca: number;
  criadoEm: string;
}

export interface Quantidade {
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  atualizadoEm?: string | null;
}

export interface Saldo {
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  reservado: number;
  disponivelParaVenda: number;
  precoAtual?: number | null;
  valorEstimado: number;
}

export interface Entrega {
  id: number;
  produtorId: number;
  produtorNome: string;
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  dataEntrega: string;
  observacao?: string | null;
  criadoEm: string;
}

export interface EntregaRequest {
  produtorId: number;
  produtoId: number;
  quantidade: number;
  dataEntrega?: string;
  observacao?: string;
}

export interface ContraProposta {
  id: number;
  autorId: number;
  autorNome: string;
  autorPerfil: Perfil;
  precoSaca: number;
  status: StatusContraProposta;
  criadoEm: string;
  respondidaEm?: string | null;
}

export interface DescontoAplicado {
  nome: string;
  valor: number;
}

export interface Transacao {
  id: number;
  requisicaoId: number;
  produtorId: number;
  produtorNome: string;
  produtorCpf: string;
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  precoSaca: number;
  valorBruto: number;
  totalDescontos: number;
  valorLiquido: number;
  status: StatusRequisicao;
  descontos: DescontoAplicado[];
  criadoEm: string;
  dataProcessamento?: string | null;
  compradoraNome?: string | null;
  compradoraDocumento?: string | null;
  compradoraEndereco?: string | null;
  compradoraCidade?: string | null;
}

export interface Requisicao {
  id: number;
  produtorId: number;
  produtorNome: string;
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  precoSaca: number;
  valorBruto: number;
  status: StatusRequisicao;
  motivoRecusa?: string | null;
  observacao?: string | null;
  aprovadoPorNome?: string | null;
  criadoEm: string;
  atualizadoEm: string;
  contraPropostas: ContraProposta[];
  transacao?: Transacao | null;
}

export interface Desconto {
  id: number;
  nome: string;
  categoria: CategoriaDesconto;
  forma: FormaDesconto;
  valor: number;
  ativo: boolean;
}

export type DescontoRequest = Omit<Desconto, 'id'>;

export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  total: number;
  totalPaginas: number;
}

export interface ProdutorResumo {
  id: number;
  nome: string;
  cpf: string;
  email: string;
  telefone?: string | null;
  cidade?: string | null;
  saldoTotalSacas: number;
  ultimaTransacaoEm?: string | null;
  ativo: boolean;
}

export interface Notificacao {
  id: number;
  tipo: string;
  mensagem: string;
  lida: boolean;
  criadoEm: string;
}

export interface Auditoria {
  id: number;
  usuarioNome?: string | null;
  usuarioEmail?: string | null;
  acao: AcaoAuditoria;
  entidade?: string | null;
  entidadeId?: number | null;
  detalhes?: string | null;
  criadoEm: string;
}

export interface DashboardProdutor {
  precos: Produto[];
  saldos: Saldo[];
  saldoTotalSacas: number;
  valorEstimadoTotal: number;
  requisicoesPendentes: number;
  ultimasRequisicoes: Requisicao[];
}

export interface CompraPorProduto {
  produto: string;
  sacas: number;
  valor: number;
}

export interface CompraPorDia {
  data: string;
  valor: number;
  sacas: number;
}

export interface DashboardCerealista {
  totalSacasCompradas: number;
  valorTotalComprado: number;
  totalDescontos: number;
  produtoresAtivos: number;
  requisicoesPendentes: number;
  comprasPorProduto: CompraPorProduto[];
  comprasPorDia: CompraPorDia[];
  pendentesRecentes: Requisicao[];
}
