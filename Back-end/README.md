# SafraLink — API REST (Spring Boot + MySQL)

API REST do SafraLink: gestão de saldos e vendas de grãos entre **produtores** e a **cerealista**.

- Java 21, Spring Boot 3.5, Spring Security (**HTTP Basic**), Spring Data JPA, Flyway, **MySQL 8**, Bean Validation, Lombok
- Pacote base: `br.com.safralink` (`domain`, `repository`, `service`, `web`, `security`, `config`)
- Documentação de requisitos: ver a ERS e a Proposta na pasta raiz do projeto

## Projeto Integrador — Padrões de Desenvolvimento Web (Engenharia de Software, 4º período)

**Projeto:** SafraLink — gestão de saldos e vendas de grãos entre produtores e cerealista.
**Objetivo do back-end:** API REST que controla produtos, preços, entregas (saldo), requisições de venda com negociação, aprovação com descontos automáticos, auditoria e notificações.

**Integrantes (404 Team):**

| Nome completo | RA |
| --- | --- |
| Leonardo Henrique Pereira Jose | 269128 |

**Entregáveis:** código em `safralink-api/` · diagrama de classes em [`docs/diagrama-de-classes.md`](docs/diagrama-de-classes.md) · modelo de dados em [`docs/modelo-dados.md`](docs/modelo-dados.md) ·
endpoints e regras em [`docs/documentacao-api.md`](docs/documentacao-api.md) ·
coleção Postman em [`postman/projeto-integrador.postman_collection.json`](postman/projeto-integrador.postman_collection.json).

## Como rodar

Pré-requisitos: **JDK 21** e **Docker** (para o MySQL).

```bash
# 1. Banco (MySQL 8.4 em localhost:3307, banco/usuário/senha "safralink"; a 3307 evita conflito com um MySQL local na 3306)
docker compose up -d

# 2. API em http://localhost:8080 (perfil dev = carrega dados de demonstração na 1ª execução)
cd safralink-api
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # Windows: mvnw.cmd ...
```

O Flyway cria as tabelas sozinho (`src/main/resources/db/migration`). Saúde da API: `GET /actuator/health`.

### Dados de demonstração (apenas perfil `dev`)

Carregados por `DadosDemoRunner` quando o banco ainda não tem usuários. **Senha de todas as contas de teste:** `Safra@2026`

| Perfil | E-mail |
| --- | --- |
| Cerealista | `cerealista@safralink.dev` |
| Produtor | `joao@safralink.dev` |
| Produtor | `maria@safralink.dev` |
| Produtor | `carlos@safralink.dev` |
| Produtor (histórico completo) | `antonio@safralink.dev` (Antônio Marcos Teixeira, Ourinhos) |
| Produtor (histórico completo) | `helena@safralink.dev` (Helena Cristina Duarte, Chavantes) |

Vêm junto: 5 produtos, 30 dias de preços, quantidades disponíveis, descontos (armazenagem 1,5% e transporte R$ 0,80/sc), entregas (saldos) e requisições em cada situação (pendente, processada e recusada).
Antônio e Helena têm cada um 6 entregas e 5 vendas ao longo de ~45 dias (pendentes, uma em negociação, aprovadas, processadas e recusada), para a área da cerealista ter painel, gráfico, transações e auditoria com conteúdo.
Todos os nomes, CPFs, telefones e valores são fictícios. Se o banco já tinha a carga base, os dois são acrescentados na próxima subida da API; para recomeçar do zero: `docker compose down -v`.
Essas contas existem só no seu banco local. **Nunca** ative `dados-demo` em produção.

## Testes

```bash
docker compose up -d
cd safralink-api && ./mvnw test
```

- `FluxoVendaTest` percorre o fluxo inteiro contra o MySQL de teste (`safralink_test`, criado pelo `docker/init.sql` na 1ª subida do container): cadastro → entrega → solicitação →
  negociação → aprovação → processamento, mais RN01/RN02/RN05/RN06 e o controle de acesso por perfil.
- `CrudTest` cobre o CRUD de produto, preço, desconto, entrega, requisição e produtor, as regras 409/422, as validações 400, os 404 e o 401/403 com HTTP Basic.
- `DocumentosTest` valida CPF/CNPJ.

## Configuração (variáveis de ambiente)

| Variável | Padrão (dev) | Observação |
| --- | --- | --- |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `jdbc:mysql://localhost:3307/safralink?...` / `safralink` / `safralink` | obrigatórias no perfil `prod` |
| `CORS_ORIGENS` | `http://localhost:4200` | lista separada por vírgula |
| `FRONTEND_URL` | `http://localhost:4200` | usado no link de recuperação de senha |

Em produção use `SPRING_PROFILES_ACTIVE=prod` (não há valores padrão para segredos) e sirva tudo por HTTPS.

## Endpoints principais (`/api`)

| Área | Endpoints | Perfil |
| --- | --- | --- |
| Autenticação | `POST /auth/registro`, `/auth/esqueci-senha`, `/auth/redefinir-senha` (público). O login é o próprio HTTP Basic: `GET /usuarios/me` devolve o usuário da credencial | público |
| Perfil | `GET/PUT /usuarios/me` | logado |
| Catálogo | `GET /produtos`, `GET /precos/historico`, `GET /quantidades` | logado |
| Preços e quantidades | `PUT /precos/{produtoId}`, `PUT /quantidades/{produtoId}` | cerealista |
| Saldos e entregas | `GET /saldos` (produtor), `GET/POST /entregas` | produtor / cerealista |
| Vendas | `GET/POST /requisicoes` (o `POST` aceita `observacao` opcional, até 255 caracteres), `POST /requisicoes/{id}/aprovar·recusar·processar·reenviar` | ambos (por ação) |
| Negociação | `GET/POST /requisicoes/{id}/contra-propostas`, `GET/PUT/DELETE /contra-propostas/{id}` (só o autor altera ou retira), `POST /contra-propostas/{id}/aceitar·recusar` | ambos |
| Gestão | `GET /transacoes[/{id}]`, `/descontos`, `/produtores`, `/dashboard/{produtor,cerealista}`, `/auditoria[/{id}]` | conforme perfil |
| Notificações | `GET /notificacoes[/{id}]`, `DELETE /notificacoes/{id}`, `POST /notificacoes/lidas` | logado |
| Relatórios | `GET /relatorios/transacoes.csv`, `/relatorios/produtores.csv` | logado / cerealista |

Erros seguem o formato `{ timestamp, status, erro, mensagem, campos? }`. A tabela completa (CRUD por entidade, regras, códigos HTTP) está em [`docs/documentacao-api.md`](docs/documentacao-api.md).

## Regras de negócio no código

| Regra | Onde |
| --- | --- |
| RN01 produtor só vende até o saldo (descontando o que já está pendente) | `SaldoService.disponivelParaVenda` / `RequisicaoService.criar` |
| RN02 aprovação só até a quantidade disponível da cerealista | `RequisicaoService.aprovar` (linha com lock pessimista) |
| RN03 preço mutável a qualquer momento (histórico preservado) | `PrecoService.definir` |
| RN04 saldo atualizado automaticamente na aprovação | `RequisicaoService.aprovar` |
| RN05 descontos automáticos na aprovação | `TransacaoService.gerar` |
| RN06 recusada volta ao início e pode ser reenviada | `RequisicaoService.reenviar` |

## Decisões que valem registrar

- **Cerealista única**, sem multi-tenant. Contas de cerealista não nascem do cadastro público (só produtor), e sim da carga de dados/banco.
- **Quem credita o saldo** é a *Entrega* registrada pela cerealista (`POST /entregas`). Estava faltando como RF na ERS.
- **Descontos entram no MVP** (a ERS os prevê e a Proposta os listava como futuro). Percentual incide sobre o valor bruto; valor fixo é R$ por saca.
- **Situações da requisição:** `PENDENTE → APROVADA → PROCESSADA`, e `PENDENTE → RECUSADA → (reenvio) PENDENTE`. `APROVADA` já debitou o saldo e gerou a transação; `PROCESSADA` é entrega/pagamento concluídos.
- **Negociação:** qualquer parte propõe um preço; só a outra parte aceita ou recusa. Aceitar atualiza o preço da requisição, que continua pendente até a decisão final.
- **Auditoria** guarda um retrato do autor (nome/e-mail), sem chave estrangeira, para sobreviver à exclusão de usuários.
- **Autenticação HTTP Basic** (enunciado do Projeto Integrador): e-mail e senha a cada requisição, sem token. Os usuários ficam no banco (BCrypt) em vez de em memória, porque cada conta tem dados próprios (saldos, vendas). 401 = sem credencial/credencial inválida; 403 = sem permissão para o papel.
- **Flyway em todos os ambientes**, com `ddl-auto: validate`: o mesmo script cria o banco no dev e na produção, evitando divergência.
- **E-mail de recuperação de senha** é simulado (o link aparece no log do servidor). Para produção, implemente `EmailService` com SMTP.
- **Relatórios** saem em CSV (abre direto no Excel).
