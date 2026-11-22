CREATE TABLE usuarios (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(150) NOT NULL,
    email       VARCHAR(180) NOT NULL,
    senha_hash  VARCHAR(255) NOT NULL,
    telefone    VARCHAR(20),
    logradouro  VARCHAR(150),
    numero      VARCHAR(20),
    bairro      VARCHAR(100),
    cidade      VARCHAR(100),
    uf          VARCHAR(2),
    cep         VARCHAR(9),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_usuarios_email UNIQUE (email));

CREATE TABLE cerealistas (
    id            BIGINT PRIMARY KEY,
    CONSTRAINT fk_cerealistas_usuario FOREIGN KEY (id) REFERENCES usuarios (id) ON DELETE CASCADE,
    cnpj          VARCHAR(14)  NOT NULL,
    razao_social  VARCHAR(180) NOT NULL,
    CONSTRAINT uk_cerealistas_cnpj UNIQUE (cnpj));

CREATE TABLE produtores (
    id             BIGINT PRIMARY KEY,
    CONSTRAINT fk_produtores_usuario FOREIGN KEY (id) REFERENCES usuarios (id) ON DELETE CASCADE,
    cpf            VARCHAR(11) NOT NULL,
    cerealista_id  BIGINT,
    CONSTRAINT uk_produtores_cpf UNIQUE (cpf),
    CONSTRAINT fk_produtores_cerealista_id FOREIGN KEY (cerealista_id) REFERENCES cerealistas (id)
);

CREATE TABLE produtos (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome     VARCHAR(60) NOT NULL,
    grao     VARCHAR(20) NOT NULL,
    mercado  VARCHAR(20) NOT NULL,
    ativo    BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_produtos_grao_mercado UNIQUE (grao, mercado));

CREATE TABLE precos (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id       BIGINT         NOT NULL,
    valor_saca       DECIMAL(14, 2) NOT NULL CHECK (valor_saca > 0),
    definido_por_id  BIGINT,
    criado_em        DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_precos_produto_id FOREIGN KEY (produto_id) REFERENCES produtos (id),
    CONSTRAINT fk_precos_definido_por_id FOREIGN KEY (definido_por_id) REFERENCES cerealistas (id)
);
CREATE INDEX idx_precos_produto_data ON precos (produto_id, criado_em DESC);

CREATE TABLE quantidades_disponiveis (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id     BIGINT         NOT NULL,
    quantidade     DECIMAL(14, 2) NOT NULL DEFAULT 0 CHECK (quantidade >= 0),
    atualizado_em  DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_quantidades_produto UNIQUE (produto_id),
    CONSTRAINT fk_quantidades_disponiveis_produto_id FOREIGN KEY (produto_id) REFERENCES produtos (id)
);

CREATE TABLE saldos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    produtor_id BIGINT         NOT NULL,
    produto_id  BIGINT         NOT NULL,
    quantidade  DECIMAL(14, 2) NOT NULL DEFAULT 0 CHECK (quantidade >= 0),
    CONSTRAINT uk_saldos_produtor_produto UNIQUE (produtor_id, produto_id),
    CONSTRAINT fk_saldos_produtor_id FOREIGN KEY (produtor_id) REFERENCES produtores (id),
    CONSTRAINT fk_saldos_produto_id FOREIGN KEY (produto_id) REFERENCES produtos (id)
);

CREATE TABLE entregas (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    produtor_id        BIGINT         NOT NULL,
    produto_id         BIGINT         NOT NULL,
    quantidade         DECIMAL(14, 2) NOT NULL CHECK (quantidade > 0),
    data_entrega       DATE           NOT NULL,
    observacao         VARCHAR(255),
    registrada_por_id  BIGINT,
    criado_em          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_entregas_produtor_id FOREIGN KEY (produtor_id) REFERENCES produtores (id),
    CONSTRAINT fk_entregas_produto_id FOREIGN KEY (produto_id) REFERENCES produtos (id),
    CONSTRAINT fk_entregas_registrada_por_id FOREIGN KEY (registrada_por_id) REFERENCES cerealistas (id)
);
CREATE INDEX idx_entregas_produtor ON entregas (produtor_id, data_entrega DESC);

CREATE TABLE requisicoes_venda (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    produtor_id     BIGINT         NOT NULL,
    produto_id      BIGINT         NOT NULL,
    quantidade      DECIMAL(14, 2) NOT NULL CHECK (quantidade > 0),
    preco_saca      DECIMAL(14, 2) NOT NULL CHECK (preco_saca > 0),
    status          VARCHAR(20)    NOT NULL,
    motivo_recusa   VARCHAR(255),
    aprovado_por_id BIGINT,
    criado_em       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    atualizado_em   DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_requisicoes_venda_produtor_id FOREIGN KEY (produtor_id) REFERENCES produtores (id),
    CONSTRAINT fk_requisicoes_venda_produto_id FOREIGN KEY (produto_id) REFERENCES produtos (id),
    CONSTRAINT fk_requisicoes_venda_aprovado_por_id FOREIGN KEY (aprovado_por_id) REFERENCES cerealistas (id)
);
CREATE INDEX idx_requisicoes_produtor ON requisicoes_venda (produtor_id, criado_em DESC);
CREATE INDEX idx_requisicoes_status ON requisicoes_venda (status, criado_em DESC);

CREATE TABLE contra_propostas (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    requisicao_id BIGINT         NOT NULL,
    autor_id      BIGINT         NOT NULL,
    preco_saca    DECIMAL(14, 2) NOT NULL CHECK (preco_saca > 0),
    status        VARCHAR(20)    NOT NULL,
    criado_em     DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    respondida_em DATETIME(6),
    CONSTRAINT fk_contra_propostas_requisicao_id FOREIGN KEY (requisicao_id) REFERENCES requisicoes_venda (id) ON DELETE CASCADE,
    CONSTRAINT fk_contra_propostas_autor_id FOREIGN KEY (autor_id) REFERENCES usuarios (id)
);
CREATE INDEX idx_contra_propostas_requisicao ON contra_propostas (requisicao_id);

CREATE TABLE descontos (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(80)    NOT NULL,
    categoria  VARCHAR(20)    NOT NULL,
    forma      VARCHAR(20)    NOT NULL,
    valor      DECIMAL(14, 4) NOT NULL CHECK (valor >= 0),
    ativo      BOOLEAN        NOT NULL DEFAULT TRUE);

CREATE TABLE transacoes (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    requisicao_id      BIGINT         NOT NULL,
    valor_bruto        DECIMAL(14, 2) NOT NULL,
    total_descontos    DECIMAL(14, 2) NOT NULL DEFAULT 0,
    valor_liquido      DECIMAL(14, 2) NOT NULL,
    data_processamento DATETIME(6),
    criado_em          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_transacoes_requisicao UNIQUE (requisicao_id),
    CONSTRAINT fk_transacoes_requisicao_id FOREIGN KEY (requisicao_id) REFERENCES requisicoes_venda (id)
);
CREATE INDEX idx_transacoes_criado_em ON transacoes (criado_em DESC);

CREATE TABLE descontos_aplicados (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transacao_id   BIGINT         NOT NULL,
    desconto_id    BIGINT,
    nome           VARCHAR(80)    NOT NULL,
    valor_aplicado DECIMAL(14, 2) NOT NULL,
    CONSTRAINT fk_descontos_aplicados_transacao_id FOREIGN KEY (transacao_id) REFERENCES transacoes (id) ON DELETE CASCADE,
    CONSTRAINT fk_descontos_aplicados_desconto_id FOREIGN KEY (desconto_id) REFERENCES descontos (id)
);
CREATE INDEX idx_descontos_aplicados_transacao ON descontos_aplicados (transacao_id);

CREATE TABLE notificacoes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL,
    tipo        VARCHAR(30)  NOT NULL,
    mensagem    VARCHAR(255) NOT NULL,
    lida        BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notificacoes_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);
CREATE INDEX idx_notificacoes_usuario ON notificacoes (usuario_id, lida, criado_em DESC);

CREATE TABLE auditorias (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id    BIGINT,
    usuario_nome  VARCHAR(150),
    usuario_email VARCHAR(180),
    acao          VARCHAR(40)  NOT NULL,
    entidade      VARCHAR(60),
    entidade_id   BIGINT,
    detalhes      VARCHAR(500),
    criado_em     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6));
CREATE INDEX idx_auditorias_criado_em ON auditorias (criado_em DESC);

CREATE TABLE tokens_recuperacao_senha (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expira_em   DATETIME(6) NOT NULL,
    usado       BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_tokens_recuperacao_senha_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);
