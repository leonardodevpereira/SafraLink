# SafraLink

Sistema web de gestão de saldos e vendas de grãos para **cerealistas** e **produtores** — 404 Team, Projeto Integrador IV (UNIFIO).

**Aplicação publicada:** https://safralink-omega.vercel.app

```
SafraLinK/
├── Front-end/Front      Angular 21 (NgModule) + Angular Material   → README próprio
├── Front-end/imagens    logos, símbolo e favicons da marca
├── Back-end/            API Spring Boot 3.5 + MySQL 8        → README próprio
│   ├── docker-compose.yml
│   └── safralink-api
└── *.docx / *.pdf       Definição Inicial do Projeto e ERS
```

## Subir tudo (desenvolvimento)

```bash
# 1. banco + API (terminal 1)
cd Back-end && docker compose up -d
cd safralink-api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 2. front (terminal 2)
cd Front-end/Front && npm install && npm start
```

Abra http://localhost:4200. As contas de demonstração (perfil `dev`) estão em `Back-end/README.md`.

Para quem vai avaliar o projeto: veja o **[AVALIACAO.md](AVALIACAO.md)** (links, acesso, roteiro de teste e mapa dos requisitos).

## Fluxo principal

1. A **cerealista** registra a **entrega** de grãos do produtor → o saldo dele é creditado.
2. O **produtor** vê saldo e preço do dia e **solicita a venda** (RN01: até o saldo disponível).
3. As partes podem **negociar o preço** por contra-propostas.
4. A **cerealista aprova ou recusa**. Na aprovação o saldo é debitado, os descontos são aplicados e nasce a transação (RN02, RN04, RN05).
5. Recusada, a requisição pode ser **reenviada** (RN06). Aprovada, é marcada como **processada** quando a entrega/pagamento fecha.
6. Tudo fica na **auditoria**, e as partes são avisadas por **notificação**.

## Cobertura da ERS

| RF | Onde |
| --- | --- |
| RF01 a RF04 login, recuperação de senha, cadastro, perfil | `auth/`, `shared/perfil` |
| RF05 notificações | sino e toast no `AppShell` |
| RF06 a RF08, RF13, RF14 dashboard, saldos, grid, preços históricos, quantidade disponível | `cliente/` |
| RF09 a RF12 solicitar venda, contra-proposta, requisições, histórico | `cliente/`, `shared/requisicao-detalhe` |
| RF15, RF24 relatórios (Excel/CSV) | Histórico (produtor), Transações e Produtores (cerealista) |
| RF16 a RF23, RF25 painel, preços, quantidade, produtores, fila, aprovação, transações, descontos, auditoria | `cerealista/` |

RNF atendidos nesta versão: senhas com bcrypt e autenticação HTTP Basic (RNF02), auditoria (RNF09), responsivo e confirmações antes de ações críticas (RNF05).
