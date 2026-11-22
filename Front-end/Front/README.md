# SafraLink — Front-end (Angular 21)

Aplicação web responsiva (320 px a 1920 px) para produtores e cerealista.

- Angular 21 em **NgModule** (`standalone: false`), SCSS, Angular Material 21 (tema `cyan` + `orange`, claro)
- Sem `zone.js` (zoneless): o estado das telas usa **signals**
- Material importado via `SharedModule`, nunca no `AppModule`; cada área é um módulo carregado sob demanda

## Como rodar

```bash
npm install
npm start          # http://localhost:4200
```

O `npm start` encaminha `/api` para `http://localhost:8080` (`proxy.conf.json`). Suba a API antes (veja `Back-end/README.md`).
Em produção sirva o front e a API na mesma origem (proxy reverso) ou ajuste o CORS da API.

```bash
npm test -- --watch=false   # testes (Vitest)
npm run build               # build de produção em dist/Front
```

## Estrutura (`src/app`)

| Pasta | Conteúdo |
| --- | --- |
| `core/` | modelos da API, serviços HTTP, sessão HTTP Basic (`AuthService`), interceptor, guards, utilitários (CPF etc.) |
| `shared/` | `SharedModule` (Material), `AppShell` (menu + notificações), `StatusChip`, `PriceChart` (SVG), diálogos, detalhe de requisição, perfil |
| `auth/` | login (tela única com seletor de perfil), cadastro, recuperação e redefinição de senha |
| `cliente/` | área do **produtor**: início, saldos, vender, requisições, histórico, preços |
| `cerealista/` | área da **cerealista**: painel, requisições, preços e quantidades, produtores, entregas, transações, descontos, auditoria |

Rotas: `/auth/*`, `/cliente/*` (perfil PRODUTOR), `/cerealista/*` (perfil CEREALISTA). Os guards levam cada perfil à própria área.

## Comportamento a conhecer

- A credencial HTTP Basic fica em `sessionStorage` (some ao fechar a aba) e o interceptor a envia a cada chamada; um `401` da API encerra a sessão e volta ao login.
- As notificações (RF05) são consultadas a cada 20 s e aparecem como aviso (toast) e no sino.
- Exportações (Excel/CSV) baixam o arquivo gerado pela API, no escopo do perfil logado.
- Contas de cerealista não têm cadastro público; use as contas de demonstração do perfil `dev` da API.
