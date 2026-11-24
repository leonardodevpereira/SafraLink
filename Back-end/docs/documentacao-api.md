# Documentação da API — SafraLink

Base: `http://localhost:8080/api`. Formato JSON. Erros: `{ "timestamp", "status", "erro", "mensagem", "campos"? }`.

## Autenticação e autorização

- **HTTP Basic** em toda requisição: `Authorization: Basic base64(e-mail:senha)`. Não há token nem sessão no servidor.
- Os usuários ficam no banco (senha com BCrypt) e são lidos por `UsuarioDetailsService`. Dois **papéis**:
  `PRODUTOR` e `CEREALISTA`. As rotas restritas usam `@PreAuthorize("hasRole('CEREALISTA')")` (ou `PRODUTOR`).
- Públicas: `POST /api/auth/**` e `GET /actuator/health`.
- **401 Unauthorized**: sem credenciais ou credenciais inválidas (ou conta inativa).
  **403 Forbidden**: autenticado, mas o papel não permite a operação (ex.: produtor criando produto).

| Conta de teste (senha `Safra@2026`) | Papel |
| --- | --- |
| `cerealista@safralink.dev` | CEREALISTA |
| `joao@safralink.dev`, `maria@safralink.dev`, `carlos@safralink.dev`, `antonio@safralink.dev`, `helena@safralink.dev` | PRODUTOR |

## Códigos de resposta usados

| Código | Quando |
| --- | --- |
| 200 / 201 / 204 | sucesso / criado / sucesso sem corpo (exclusões) |
| 400 | dados inválidos (Bean Validation) ou requisição mal formada |
| 401 / 403 | não autenticado / sem permissão |
| 404 | recurso inexistente |
| 409 | conflito com regra de negócio ou integridade (duplicidade, vínculos, estado incompatível) |
| 422 | regra de negócio violada (ex.: quantidade acima do saldo) |

## Endpoints (CRUD por entidade)

| Entidade | Criar | Ler | Atualizar | Excluir |
| --- | --- | --- | --- | --- |
| **Produto** | `POST /produtos` (C) | `GET /produtos?grao=`, `GET /produtos/{id}` | `PUT /produtos/{id}` (C) | `DELETE /produtos/{id}` (C) |
| **Preço** | `POST /precos` (C) | `GET /precos/{id}`, `GET /precos/historico?produtoId=&dias=` | `PUT /precos/{produtoId}` (C): publica o novo preço vigente (o histórico é imutável, RN03) | `DELETE /precos/{id}` (C) |
| **Quantidade disponível** | criada junto com o produto | `GET /quantidades?abaixoDe=`, `GET /quantidades/{produtoId}` | `PUT /quantidades/{produtoId}` (C) | removida junto com o produto |
| **Desconto** | `POST /descontos` (C) | `GET /descontos`, `GET /descontos/{id}` | `PUT /descontos/{id}` (C) | `DELETE /descontos/{id}` (C) |
| **Produtor** | `POST /produtores` (C); autocadastro em `POST /auth/registro` | `GET /produtores?busca=`, `GET /produtores/{id}` (C) | `PUT /produtores/{id}` (C); o próprio usuário em `PUT /usuarios/me` | `DELETE /produtores/{id}` (C): exclusão lógica |
| **Entrega** | `POST /entregas` (C) | `GET /entregas`, `GET /entregas/{id}` | `PUT /entregas/{id}` (C) | `DELETE /entregas/{id}` (C) |
| **Requisição de venda** | `POST /requisicoes` (P) | `GET /requisicoes?status=&produtorId=`, `GET /requisicoes/{id}` | `PUT /requisicoes/{id}` (P, só pendente) | `DELETE /requisicoes/{id}` (P, pendente ou recusada) |
| **Contra-proposta** | `POST /requisicoes/{id}/contra-propostas` (as duas partes, requisição pendente) | `GET /requisicoes/{id}/contra-propostas`, `GET /contra-propostas/{id}` | `PUT /contra-propostas/{id}` (só o autor, enquanto pendente); a outra parte responde em `POST /contra-propostas/{id}/aceitar·recusar` | `DELETE /contra-propostas/{id}` (só o autor, enquanto pendente) |
| **Notificação** | gerada pelo sistema a cada evento | `GET /notificacoes`, `GET /notificacoes/nao-lidas`, `GET /notificacoes/{id}` | `POST /notificacoes/{id}/lida`, `POST /notificacoes/lidas` | `DELETE /notificacoes/{id}`, `DELETE /notificacoes` (só as do próprio usuário) |

(C) = somente CEREALISTA · (P) = somente PRODUTOR · sem marca = qualquer usuário autenticado (o produtor só enxerga o que é dele).

Entidades **derivadas** (criadas e mantidas pelo próprio sistema; a API oferece só leitura, de propósito, para não
quebrar a integridade do negócio e da auditoria): `Saldo` (`GET /saldos`, `GET /saldos/{produtoId}` (P); só muda por entrega, venda e ajuste),
`Transacao` e `DescontoAplicado` (`GET /transacoes`, `GET /transacoes/{id}`; nascem na aprovação da venda) e
`Auditoria` (`GET /auditoria`, `GET /auditoria/{id}` (C); registro imutável).

## Funcionalidade específica do PI — fluxo de venda de grãos

| Passo | Endpoint | Quem | O que acontece |
| --- | --- | --- | --- |
| 1 | `POST /entregas` | cerealista | credita o saldo (sacas) do produtor |
| 2 | `POST /requisicoes` | produtor | solicita a venda; o saldo disponível já desconta as pendentes (RN01) |
| 3 | `POST /requisicoes/{id}/contra-propostas` · `/contra-propostas/{id}/aceitar·recusar` | ambos | negociação de preço |
| 4 | `POST /requisicoes/{id}/aprovar` | cerealista | confere disponibilidade e saldo (RN02), debita o saldo (RN04), gera a **transação** com descontos automáticos (RN05) |
| 5 | `POST /requisicoes/{id}/processar` | cerealista | conclui entrega/pagamento |
| — | `POST /requisicoes/{id}/recusar` · `/reenviar` | cerealista / produtor | recusa com motivo e reenvio (RN06) |

Outros: `GET /transacoes`, `GET /dashboard/{produtor,cerealista}`, `GET /auditoria` (C), `GET/POST/DELETE /notificacoes`,
`GET /relatorios/transacoes.csv`, `GET /relatorios/produtores.csv`.

## Regras de negócio (camada de serviço)

| Regra | Resposta | Onde |
| --- | --- | --- |
| Não existem dois produtos com o mesmo grão e mercado | 409 | `ProdutoService.criar/atualizar` |
| Produto com preços, entregas, saldos ou requisições não pode ser excluído | 409 | `ProdutoService.excluir` |
| O único preço de um produto não pode ser excluído | 409 | `PrecoService.excluir` |
| Desconto já aplicado em transação não pode ser excluído (inative-o) | 409 | `DescontoService.excluir` |
| Produtor com saldo ou requisição pendente não pode ser excluído | 409 | `ProdutorService.excluir` |
| Reduzir/excluir entrega que deixaria o saldo abaixo do vendido ou reservado | 409 | `SaldoService.ajustar` |
| Requisição aprovada/processada não pode ser editada nem excluída | 409 | `RequisicaoService` |
| Venda acima do saldo disponível (descontadas as pendentes) | 422 | `RequisicaoService.criar/atualizar` |
| Aprovação acima da quantidade disponível da cerealista ou do saldo do produtor | 422 | `RequisicaoService.aprovar` |
| Data de entrega no futuro | 422 | `EntregaService` |

## Consultas personalizadas (Spring Data JPA)

`ProdutoRepository.findByGraoAndAtivoTrueOrderByIdAsc` (derived query) · `QuantidadeDisponivelRepository.abaixoDe` (JPQL
`@Query`) · `RequisicaoVendaRepository` com `Specification` (status/produtor) e `@Query` de soma de reservas ·
`SaldoRepository.totaisPorProdutor` (JPQL com `group by`) · `PrecoRepository.findByProdutoIdAndCriadoEmGreaterThanEqual...`.

## Validação e exceções

Bean Validation nos DTOs (`@NotNull`, `@NotBlank`, `@Size`, `@DecimalMin`, `@Digits`, `@Email`), acionada com `@Valid`.
`ApiException` (exceção de negócio com status HTTP) e `GlobalExceptionHandler` (`@RestControllerAdvice`) padronizam as
respostas 400/404/409/422; `SecurityConfig` padroniza 401/403.

## Postman

Coleção: [`postman/projeto-integrador.postman_collection.json`](../postman/projeto-integrador.postman_collection.json)
(importar no Postman; 92 requisições em 8 pastas, com respostas salvas e as variáveis da coleção incluídas).
Execute as pastas em ordem, com a API no ar e `safralink.dados-demo=true`. O arquivo é regenerado (com respostas reais)
por `python postman/gerar_colecao.py`.
