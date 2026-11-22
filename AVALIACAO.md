# SafraLink — Guia para avaliação

Projeto Integrador IV — **404 Team** — UNIFIO.
Sistema web de gestão de saldos e vendas de grãos entre **cerealistas** e **produtores rurais**.

## 1. Links

| Item | Endereço |
| --- | --- |
| Aplicação publicada | _preencher (link do deploy)_ |
| Repositório Git | https://github.com/leonardodevpereira/SafraLink |

## 2. Integrantes

| Nome completo | RA |
| --- | --- |
| Leonardo Henrique Pereira Jose | 269128 |

## 3. Acesso para teste

Senha de todas as contas: `Safra@2026`

| Perfil | E-mail | Observação |
| --- | --- | --- |
| Cerealista | `cerealista@safralink.dev` | na tela de login, marque **"Sou cerealista"** antes de entrar |
| Produtor | `antonio@safralink.dev` | histórico completo de entregas e vendas |
| Produtor | `helena@safralink.dev` | histórico completo de entregas e vendas |
| Produtor | `carlos@safralink.dev` | usado no roteiro abaixo |

Todos os dados (nomes, CPFs, valores) são fictícios.

## 4. Roteiro do fluxo principal (cerca de 8 passos)

1. **Cerealista** entra e vê o **Painel**: total comprado, saldo em custódia, produtores ativos, requisições pendentes, gráfico de compras dos últimos 30 dias e compras por produto.
2. Em **Entregas**, registra uma entrega para **Carlos Souza** (produto "Milho Interno", 100 sacas). O saldo do produtor é creditado e ele é notificado.
3. Em **Preços e quantidades**, confere o preço do dia e a quantidade disponível para compra de "Milho Interno" (a aprovação só acontece até essa quantidade).
4. Sai e entra como **produtor** `carlos@safralink.dev`. Em **Meus saldos** vê o crédito. Em **Vender**, solicita a venda de parte do saldo (ex.: 50 sacas). O sistema só aceita até o saldo disponível.
5. Em **Minhas requisições**, a solicitação aparece como **Pendente**. Dá para editar, cancelar ou abrir os detalhes e negociar o preço por contra-proposta.
6. Volta como **cerealista**, abre **Requisições** e **aprova** a venda. O saldo do produtor é debitado, os descontos ativos (armazenagem e transporte) são aplicados e nasce a transação.
7. Em **Transações**, clica na linha da venda: aparecem os **detalhes** (valor bruto, descontos, líquido) e a aba **Contrato de venda**, com opção de imprimir ou salvar em PDF.
8. Como produtor, em **Histórico**, abre a mesma venda e vê o mesmo contrato. A cerealista também consegue conferir tudo na **Auditoria**.

Outros caminhos: recusar uma venda (com motivo) e o produtor reenviá-la; marcar a venda aprovada como processada; exportar relatórios em CSV (Excel).

## 5. Requisitos da entrega e onde estão

| Requisito | Atendimento |
| --- | --- |
| Angular com `standalone: false` e NgModules | todos os componentes são declarados em módulos: `SharedModule`, `AuthModule`, `ClienteModule` e `CerealistaModule` (carregados por lazy loading) em `Front-end/Front/src/app/` |
| Roteamento habilitado, SSR/SSG desabilitado | roteamento por módulo; o `angular.json` não tem configuração de SSR nem de prerender |
| Angular Material | tabelas, diálogos, selects, autocomplete, botões, chips, menus, paginador, snackbar etc., reexportados pelo `SharedModule` |
| 4 ou mais entidades relacionadas | 16 entidades: `Usuario` (`Produtor`, `Cerealista`), `Produto`, `Preco`, `QuantidadeDisponivel`, `Saldo`, `Entrega`, `RequisicaoVenda`, `ContraProposta`, `Transacao`, `Desconto`, `DescontoAplicado`, `Notificacao`, `Auditoria`, `TokenRecuperacaoSenha` |
| CRUD completo das entidades principais | **Produtos**, **Descontos**, **Entregas**, **Produtores** (menus da cerealista) e **Requisições** (área do produtor) têm cadastro, edição e exclusão pela interface. Os preços são lançados em "Preços e quantidades" |
| Dashboard com 3 ou mais KPIs | Painel da cerealista (total comprado, saldo em custódia, produtores ativos, requisições pendentes, gráfico de 30 dias, compras por produto) e Início do produtor (resumo geral, cartão por grão, preços e saldos) |
| Fluxo principal de ponta a ponta | roteiro da seção 4 |

## 6. Documentação técnica

| Documento | Caminho |
| --- | --- |
| Diagrama de classes | [`Back-end/docs/diagrama-de-classes.md`](Back-end/docs/diagrama-de-classes.md) |
| Modelo de dados (entidade-relacionamento) | [`Back-end/docs/modelo-dados.md`](Back-end/docs/modelo-dados.md) |
| Endpoints, regras de negócio e códigos HTTP | [`Back-end/docs/documentacao-api.md`](Back-end/docs/documentacao-api.md) |
| Coleção do Postman (64 requisições com respostas salvas) | [`Back-end/postman/projeto-integrador.postman_collection.json`](Back-end/postman/projeto-integrador.postman_collection.json) |
| README do back-end (execução, configuração, decisões) | [`Back-end/README.md`](Back-end/README.md) |
| README do front-end | [`Front-end/Front/README.md`](Front-end/Front/README.md) |

Os diagramas estão em Mermaid, que o GitHub exibe renderizado.

## 7. Tecnologias

- **Front-end:** Angular 21 (NgModules), Angular Material, SCSS, RxJS, Vitest.
- **Back-end:** Java 21, Spring Boot 3.5, Spring Security (HTTP Basic), Spring Data JPA, Flyway, Bean Validation.
- **Banco de dados:** MySQL 8.

## 8. Como rodar localmente (opcional)

Pré-requisitos: JDK 21, Node.js e Docker.

```bash
cd Back-end && docker compose up -d
cd safralink-api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--safralink.dados-demo=true

cd Front-end/Front && npm install && npm start
```

Abra http://localhost:4200. Testes: `./mvnw test` (back-end, com o MySQL do Docker no ar) e `npm test` (front-end).
