# Modelo de dados — SafraLink

Diagrama entidade-relacionamento do banco MySQL (16 entidades JPA). O diagrama é Mermaid: o GitHub e o VS Code
(extensão *Markdown Preview Mermaid Support*) o exibem renderizado.

```mermaid
erDiagram
    USUARIO ||--o| PRODUTOR : "e um (JOINED)"
    USUARIO ||--o| CEREALISTA : "e um (JOINED)"
    CEREALISTA ||--o{ PRODUTOR : "atende"
    USUARIO ||--o{ NOTIFICACAO : recebe
    USUARIO ||--o{ TOKEN_RECUPERACAO_SENHA : solicita

    PRODUTO ||--o{ PRECO : "tem historico de"
    PRODUTO ||--|| QUANTIDADE_DISPONIVEL : "limite de compra"
    CEREALISTA ||--o{ PRECO : define

    PRODUTOR ||--o{ SALDO : possui
    PRODUTO ||--o{ SALDO : "em sacas de"
    PRODUTOR ||--o{ ENTREGA : realiza
    PRODUTO ||--o{ ENTREGA : "de"
    CEREALISTA ||--o{ ENTREGA : registra

    PRODUTOR ||--o{ REQUISICAO_VENDA : solicita
    PRODUTO ||--o{ REQUISICAO_VENDA : "de"
    CEREALISTA |o--o{ REQUISICAO_VENDA : "aprova/recusa"
    REQUISICAO_VENDA ||--o{ CONTRA_PROPOSTA : negocia
    USUARIO ||--o{ CONTRA_PROPOSTA : propoe
    REQUISICAO_VENDA ||--o| TRANSACAO : "gera ao aprovar"
    TRANSACAO ||--o{ DESCONTO_APLICADO : "tem"
    DESCONTO |o--o{ DESCONTO_APLICADO : "origem"

    USUARIO {
        bigint id PK
        string nome
        string email UK
        string senha_hash "BCrypt"
        string telefone
        embedded endereco
        boolean ativo
        datetime criado_em
    }
    PRODUTOR {
        bigint id PK,FK
        string cpf UK
        bigint cerealista_id FK
    }
    CEREALISTA {
        bigint id PK,FK
        string cnpj UK
        string razao_social
    }
    PRODUTO {
        bigint id PK
        string nome
        enum grao "SOJA, MILHO, FEIJAO"
        enum mercado "INTERNO, EXTERNO"
        boolean ativo
    }
    PRECO {
        bigint id PK
        bigint produto_id FK
        decimal valor_saca
        bigint definido_por_id FK
        datetime criado_em
    }
    QUANTIDADE_DISPONIVEL {
        bigint id PK
        bigint produto_id FK,UK
        decimal quantidade
        datetime atualizado_em
    }
    SALDO {
        bigint id PK
        bigint produtor_id FK
        bigint produto_id FK
        decimal quantidade
    }
    ENTREGA {
        bigint id PK
        bigint produtor_id FK
        bigint produto_id FK
        decimal quantidade
        date data_entrega
        string observacao
        bigint registrada_por_id FK
    }
    REQUISICAO_VENDA {
        bigint id PK
        bigint produtor_id FK
        bigint produto_id FK
        decimal quantidade
        decimal preco_saca
        enum status "PENDENTE, APROVADA, PROCESSADA, RECUSADA"
        string motivo_recusa
        string observacao
        bigint aprovado_por_id FK
    }
    CONTRA_PROPOSTA {
        bigint id PK
        bigint requisicao_id FK
        bigint autor_id FK
        decimal preco_saca
        enum status
    }
    DESCONTO {
        bigint id PK
        string nome
        enum categoria
        enum forma "PERCENTUAL, VALOR_FIXO"
        decimal valor
        boolean ativo
    }
    TRANSACAO {
        bigint id PK
        bigint requisicao_id FK,UK
        decimal valor_bruto
        decimal total_descontos
        decimal valor_liquido
        datetime data_processamento
    }
    DESCONTO_APLICADO {
        bigint id PK
        bigint transacao_id FK
        bigint desconto_id FK
        string nome
        decimal valor_aplicado
    }
    NOTIFICACAO {
        bigint id PK
        bigint usuario_id FK
        enum tipo
        string mensagem
        boolean lida
    }
    AUDITORIA {
        bigint id PK
        string usuario_email
        enum acao
        string entidade
        bigint entidade_id
        string detalhes
    }
    TOKEN_RECUPERACAO_SENHA {
        bigint id PK
        bigint usuario_id FK
        string token_hash UK
        datetime expira_em
        boolean usado
    }
```

## Leitura rápida

| Grupo | Entidades | Papel no domínio |
| --- | --- | --- |
| Pessoas | `Usuario` → `Produtor` / `Cerealista` | herança JPA `JOINED`; o papel (`ROLE_PRODUTOR` / `ROLE_CEREALISTA`) vem do tipo |
| Catálogo | `Produto`, `Preco`, `QuantidadeDisponivel` | grão + mercado; preço com histórico; limite de compra da cerealista |
| Estoque do produtor | `Saldo`, `Entrega` | a entrega registrada pela cerealista credita o saldo em sacas |
| Venda | `RequisicaoVenda`, `ContraProposta`, `Transacao`, `DescontoAplicado`, `Desconto` | fluxo solicitar → negociar → aprovar → processar, com descontos automáticos |
| Apoio | `Notificacao`, `Auditoria`, `TokenRecuperacaoSenha` | avisos, trilha de auditoria e recuperação de senha |

Integridade: chaves estrangeiras e `CHECK` (quantidades e preços positivos) no próprio schema
(`src/main/resources/db/migration`), com `ddl-auto: validate` para o Hibernate conferir o mapeamento na subida.
