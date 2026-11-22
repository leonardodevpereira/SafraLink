# Diagrama de classes — SafraLink

Classes de domínio do back-end (pacote `br.com.safralink.domain`), com atributos principais, métodos de negócio e
relacionamentos. O diagrama é Mermaid: o GitHub e o VS Code (extensão *Markdown Preview Mermaid Support*) o exibem
renderizado. O modelo relacional correspondente está em [`modelo-dados.md`](modelo-dados.md).

## 1. Domínio

```mermaid
classDiagram
    direction TB

    class Usuario {
        <<abstract>>
        -Long id
        -String nome
        -String email
        -String senhaHash
        -String telefone
        -Endereco endereco
        -boolean ativo
        -Instant criadoEm
        +getPerfil() Perfil*
        +getDocumento() String*
    }
    class Produtor {
        -String cpf
        +getPerfil() Perfil
        +getDocumento() String
    }
    class Cerealista {
        -String cnpj
        -String razaoSocial
        +getPerfil() Perfil
        +getDocumento() String
    }
    class Endereco {
        <<embeddable>>
        -String logradouro
        -String numero
        -String bairro
        -String cidade
        -String uf
        -String cep
    }

    class Produto {
        -Long id
        -String nome
        -TipoGrao grao
        -Mercado mercado
        -boolean ativo
    }
    class Preco {
        -Long id
        -BigDecimal valorSaca
        -Instant criadoEm
    }
    class QuantidadeDisponivel {
        -Long id
        -BigDecimal quantidade
        -Instant atualizadoEm
    }
    class Saldo {
        -Long id
        -BigDecimal quantidade
        +creditar(BigDecimal)
        +debitar(BigDecimal)
    }
    class Entrega {
        -Long id
        -BigDecimal quantidade
        -LocalDate dataEntrega
        -String observacao
        -Instant criadoEm
    }

    class RequisicaoVenda {
        -Long id
        -BigDecimal quantidade
        -BigDecimal precoSaca
        -StatusRequisicao status
        -String motivoRecusa
        -String observacao
        -Instant criadoEm
        -Instant atualizadoEm
        +valorBruto() BigDecimal
    }
    class ContraProposta {
        -Long id
        -BigDecimal precoSaca
        -StatusContraProposta status
        -Instant criadoEm
        -Instant respondidaEm
    }
    class Transacao {
        -Long id
        -BigDecimal valorBruto
        -BigDecimal totalDescontos
        -BigDecimal valorLiquido
        -Instant dataProcessamento
        +adicionarDesconto(DescontoAplicado)
    }
    class Desconto {
        -Long id
        -String nome
        -CategoriaDesconto categoria
        -FormaDesconto forma
        -BigDecimal valor
        -boolean ativo
    }
    class DescontoAplicado {
        -Long id
        -String nome
        -BigDecimal valorAplicado
    }

    class Notificacao {
        -Long id
        -TipoNotificacao tipo
        -String mensagem
        -boolean lida
        -Instant criadoEm
    }
    class Auditoria {
        -Long id
        -AcaoAuditoria acao
        -String usuarioEmail
        -String entidade
        -Long entidadeId
        -String detalhes
        -Instant criadoEm
    }
    class TokenRecuperacaoSenha {
        -Long id
        -String tokenHash
        -Instant expiraEm
        -boolean usado
    }

    class Perfil {
        <<enumeration>>
        PRODUTOR
        CEREALISTA
    }
    class TipoGrao {
        <<enumeration>>
        SOJA
        MILHO
        FEIJAO
    }
    class Mercado {
        <<enumeration>>
        INTERNO
        EXTERNO
    }
    class StatusRequisicao {
        <<enumeration>>
        PENDENTE
        APROVADA
        RECUSADA
        PROCESSADA
    }
    class StatusContraProposta {
        <<enumeration>>
        PENDENTE
        ACEITA
        RECUSADA
        CANCELADA
    }
    class FormaDesconto {
        <<enumeration>>
        PERCENTUAL
        VALOR_FIXO
    }
    class CategoriaDesconto {
        <<enumeration>>
        ARMAZENAGEM
        TRANSPORTE
        OUTRO
    }

    Usuario <|-- Produtor
    Usuario <|-- Cerealista
    Usuario *-- Endereco
    Cerealista "1" o-- "0..*" Produtor : atende

    Produto "1" o-- "0..*" Preco : historico
    Produto "1" -- "1" QuantidadeDisponivel : limite de compra
    Cerealista "1" -- "0..*" Preco : define
    Produtor "1" -- "0..*" Saldo : possui
    Produto "1" -- "0..*" Saldo
    Produtor "1" -- "0..*" Entrega : realiza
    Produto "1" -- "0..*" Entrega
    Cerealista "1" -- "0..*" Entrega : registra

    Produtor "1" -- "0..*" RequisicaoVenda : solicita
    Produto "1" -- "0..*" RequisicaoVenda
    Cerealista "0..1" -- "0..*" RequisicaoVenda : aprova ou recusa
    RequisicaoVenda "1" *-- "0..*" ContraProposta : negociacao
    Usuario "1" -- "0..*" ContraProposta : autor
    RequisicaoVenda "1" -- "0..1" Transacao : gera ao aprovar
    Transacao "1" *-- "0..*" DescontoAplicado
    Desconto "0..1" -- "0..*" DescontoAplicado : origem

    Usuario "1" -- "0..*" Notificacao : recebe
    Usuario "1" -- "0..*" TokenRecuperacaoSenha : solicita

    Usuario ..> Perfil
    Produto ..> TipoGrao
    Produto ..> Mercado
    RequisicaoVenda ..> StatusRequisicao
    ContraProposta ..> StatusContraProposta
    Desconto ..> FormaDesconto
    Desconto ..> CategoriaDesconto
```

`Auditoria` é independente de propósito: guarda um retrato do autor (nome e e-mail) sem chave estrangeira, para
sobreviver à exclusão de usuários.

### Ciclo de vida da requisição de venda

```mermaid
stateDiagram-v2
    [*] --> PENDENTE : produtor solicita
    PENDENTE --> APROVADA : cerealista aprova (debita saldo, gera Transacao)
    PENDENTE --> RECUSADA : cerealista recusa (com motivo)
    RECUSADA --> PENDENTE : produtor reenvia
    APROVADA --> PROCESSADA : cerealista processa
    PROCESSADA --> [*]
```

## 2. Camadas da aplicação

```mermaid
classDiagram
    direction LR

    class Controller {
        <<web>>
        CatalogoController
        SaldoEntregaController
        RequisicaoController
        GestaoController
        NotificacaoRelatorioController
        UsuarioController
        AuthController
    }
    class Service {
        <<service>>
        ProdutoService
        PrecoService
        QuantidadeService
        SaldoService
        EntregaService
        RequisicaoService
        TransacaoService
        DescontoService
        ProdutorService
        DashboardService
        NotificacaoService
        AuditoriaService
        AuthService
    }
    class Repository {
        <<repository>>
        ProdutoRepository
        PrecoRepository
        SaldoRepository
        EntregaRepository
        RequisicaoVendaRepository
        TransacaoRepository
        UsuarioRepository
        demaisRepositorios
    }
    class Entity {
        <<domain>>
    }
    class Seguranca {
        <<security>>
        SecurityConfig
        UsuarioDetailsService
        UsuarioPrincipal
    }
    class Excecoes {
        <<web>>
        ApiException
        GlobalExceptionHandler
    }

    Controller --> Service : chama
    Service --> Repository : acessa dados
    Repository --> Entity : persiste
    Controller ..> Seguranca : papel exigido
    Service ..> Excecoes : lanca ApiException
```

Regra de arquitetura: os controllers não acessam repositórios; todo acesso a dados e toda regra de negócio passam pela
camada de serviço.
