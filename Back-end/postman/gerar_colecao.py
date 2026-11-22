"""
Gera postman/projeto-integrador.postman_collection.json executando cada requisição contra a API em execução
(http://localhost:8080) e gravando a resposta real como "Save Response" (exemplo salvo) em cada item.

Uso (com a API no ar e os dados de demonstração carregados):
    python postman/gerar_colecao.py
"""
import base64
import json
import random
import time
import urllib.error
import urllib.request
from pathlib import Path

BASE = "http://localhost:8080"
SENHA = "Safra@2026"
VARS = {
    "baseUrl": BASE,
    "cerealistaEmail": "cerealista@safralink.dev",
    "cerealistaSenha": SENHA,
    "produtorEmail": "carlos@safralink.dev",
    "produtorSenha": SENHA,
    "produtoSojaInternoId": "1",
    "produtoMilhoExternoId": "4",
    "produtorCarlosId": "4",
}
RUN = dict(VARS)

def cpf_valido():
    d = [random.randint(0, 9) for _ in range(9)]
    if len(set(d)) == 1:
        d[0] = (d[0] + 1) % 10
    for j in (9, 10):
        soma = sum(d[i] * (j + 1 - i) for i in range(j))
        r = (soma * 10) % 11
        d.append(0 if r == 10 else r)
    return "".join(map(str, d))

def subst(texto):
    if not isinstance(texto, str):
        return texto
    for k, v in RUN.items():
        texto = texto.replace("{{%s}}" % k, str(v))
    return texto

def auth_header(quem):
    if quem == "cer":
        cred = (RUN["cerealistaEmail"], RUN["cerealistaSenha"])
    elif quem == "pro":
        cred = (RUN["produtorEmail"], RUN["produtorSenha"])
    elif quem == "errada":
        cred = (RUN["cerealistaEmail"], "senha-incorreta")
    else:
        return None
    return "Basic " + base64.b64encode(f"{cred[0]}:{cred[1]}".encode()).decode()

def chamar(metodo, caminho, quem, corpo):
    url = subst(caminho)
    dados = json.dumps(json.loads(subst(json.dumps(corpo))), ensure_ascii=False).encode() if corpo is not None else None
    req = urllib.request.Request(url, data=dados, method=metodo)
    req.add_header("Accept", "application/json")
    if dados is not None:
        req.add_header("Content-Type", "application/json")
    h = auth_header(quem)
    if h:
        req.add_header("Authorization", h)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, r.reason, r.read().decode("utf-8"), dict(r.headers)
    except urllib.error.HTTPError as e:
        return e.code, e.reason, e.read().decode("utf-8"), dict(e.headers)

def pegar(obj, caminho):
    for parte in caminho.split("."):
        obj = obj[parte]
    return obj

PASSOS = []

def p(pasta, nome, metodo, caminho, quem, corpo, esperado, guardar=None, pre=None):
    PASSOS.append((pasta, nome, metodo, caminho, quem, corpo, esperado, guardar, pre))

A = "1. Autenticação e autorização (401 / 403)"
p(A, "401 - Sem credenciais (não autenticado)", "GET", "{{baseUrl}}/api/produtos", "none", None, 401)
p(A, "401 - Credenciais inválidas (senha errada)", "GET", "{{baseUrl}}/api/usuarios/me", "errada", None, 401)
p(A, "200 - Autenticação válida: cerealista", "GET", "{{baseUrl}}/api/usuarios/me", "cer", None, 200)
p(A, "200 - Autenticação válida: produtor", "GET", "{{baseUrl}}/api/usuarios/me", "pro", None, 200)
p(A, "403 - Sem permissão: produtor tenta criar produto", "POST", "{{baseUrl}}/api/produtos", "pro",
  {"nome": "Soja Teste", "grao": "SOJA", "mercado": "EXTERNO"}, 403)
p(A, "403 - Sem permissão: produtor tenta listar produtores", "GET", "{{baseUrl}}/api/produtores", "pro", None, 403)

P = "2. Produtos - CRUD, validações e regras"
p(P, "201 - Criar produto", "POST", "{{baseUrl}}/api/produtos", "cer",
  {"nome": "Feijão Externo (Postman)", "grao": "FEIJAO", "mercado": "EXTERNO"}, 201, ("produtoId", "id"))
p(P, "409 - Regra: produto duplicado (mesmo grão e mercado)", "POST", "{{baseUrl}}/api/produtos", "cer",
  {"nome": "Duplicado", "grao": "FEIJAO", "mercado": "EXTERNO"}, 409)
p(P, "400 - Validação: nome em branco", "POST", "{{baseUrl}}/api/produtos", "cer",
  {"nome": "", "grao": "FEIJAO", "mercado": "EXTERNO"}, 400)
p(P, "200 - Buscar produto por id", "GET", "{{baseUrl}}/api/produtos/{{produtoId}}", "pro", None, 200)
p(P, "200 - Listar produtos", "GET", "{{baseUrl}}/api/produtos", "pro", None, 200)
p(P, "200 - Consulta personalizada: produtos por grão", "GET", "{{baseUrl}}/api/produtos?grao=SOJA", "pro", None, 200)
p(P, "200 - Atualizar produto", "PUT", "{{baseUrl}}/api/produtos/{{produtoId}}", "cer",
  {"nome": "Feijão Externo Atualizado", "grao": "FEIJAO", "mercado": "EXTERNO"}, 200)
p(P, "404 - Produto inexistente", "GET", "{{baseUrl}}/api/produtos/999999", "pro", None, 404)
p(P, "409 - Regra: produto com vínculos não pode ser excluído", "DELETE", "{{baseUrl}}/api/produtos/{{produtoSojaInternoId}}",
  "cer", None, 409)
p(P, "204 - Excluir produto sem vínculos", "DELETE", "{{baseUrl}}/api/produtos/{{produtoId}}", "cer", None, 204)
p(P, "404 - Produto já excluído", "GET", "{{baseUrl}}/api/produtos/{{produtoId}}", "cer", None, 404)

Q = "3. Preços e quantidades - CRUD e consultas"
p(Q, "201 - Criar preço (novo preço do dia)", "POST", "{{baseUrl}}/api/precos", "cer",
  {"produtoId": "{{produtoMilhoExternoId}}", "valorSaca": 71.5}, 201, ("precoId", "id"))
p(Q, "400 - Validação: preço zero", "POST", "{{baseUrl}}/api/precos", "cer",
  {"produtoId": "{{produtoMilhoExternoId}}", "valorSaca": 0}, 400)
p(Q, "200 - Buscar preço por id", "GET", "{{baseUrl}}/api/precos/{{precoId}}", "pro", None, 200)
p(Q, "200 - Histórico de preços (consulta personalizada)", "GET",
  "{{baseUrl}}/api/precos/historico?produtoId={{produtoMilhoExternoId}}&dias=30", "pro", None, 200)
p(Q, "200 - Atualizar o preço vigente (PUT por produto)", "PUT", "{{baseUrl}}/api/precos/{{produtoMilhoExternoId}}", "cer",
  {"valorSaca": 72.0}, 200, ("precoAtualizadoId", "id"))
p(Q, "204 - Excluir preço criado", "DELETE", "{{baseUrl}}/api/precos/{{precoId}}", "cer", None, 204)
p(Q, "404 - Preço excluído não existe mais", "GET", "{{baseUrl}}/api/precos/{{precoId}}", "cer", None, 404)
p(Q, "200 - Definir quantidade disponível para compra", "PUT", "{{baseUrl}}/api/quantidades/{{produtoMilhoExternoId}}",
  "cer", {"quantidade": 5000}, 200)
p(Q, "200 - Buscar quantidade por produto", "GET", "{{baseUrl}}/api/quantidades/{{produtoMilhoExternoId}}", "pro", None, 200)
p(Q, "200 - Consulta personalizada: quantidade abaixo de um limite", "GET", "{{baseUrl}}/api/quantidades?abaixoDe=100",
  "cer", None, 200)

D = "4. Descontos - CRUD"
p(D, "201 - Criar desconto", "POST", "{{baseUrl}}/api/descontos", "cer",
  {"nome": "Taxa de teste Postman", "categoria": "ARMAZENAGEM", "forma": "PERCENTUAL", "valor": 1.5}, 201,
  ("descontoId", "id"))
p(D, "400 - Validação: percentual acima de 100%", "POST", "{{baseUrl}}/api/descontos", "cer",
  {"nome": "Inválido", "categoria": "ARMAZENAGEM", "forma": "PERCENTUAL", "valor": 150}, 400)
p(D, "200 - Buscar desconto", "GET", "{{baseUrl}}/api/descontos/{{descontoId}}", "pro", None, 200)
p(D, "200 - Atualizar desconto", "PUT", "{{baseUrl}}/api/descontos/{{descontoId}}", "cer",
  {"nome": "Taxa de teste atualizada", "categoria": "ARMAZENAGEM", "forma": "PERCENTUAL", "valor": 2.0, "ativo": True}, 200)
p(D, "403 - Produtor não exclui desconto", "DELETE", "{{baseUrl}}/api/descontos/{{descontoId}}", "pro", None, 403)
p(D, "204 - Excluir desconto", "DELETE", "{{baseUrl}}/api/descontos/{{descontoId}}", "cer", None, 204)
p(D, "404 - Desconto excluído", "GET", "{{baseUrl}}/api/descontos/{{descontoId}}", "cer", None, 404)

R = "5. Produtores - CRUD"
p(R, "201 - Cerealista cadastra produtor", "POST", "{{baseUrl}}/api/produtores", "cer",
  {"nome": "Produtor Postman", "email": "postman-{{sufixo}}@teste.dev", "senha": "Teste@12345", "cpf": "{{cpfGerado}}"},
  201, ("novoProdutorId", "id"), pre="cadastro")
p(R, "409 - E-mail já cadastrado", "POST", "{{baseUrl}}/api/produtores", "cer",
  {"nome": "Repetido", "email": "postman-{{sufixo}}@teste.dev", "senha": "Teste@12345", "cpf": "{{cpfGerado}}"}, 409)
p(R, "400 - CPF inválido", "POST", "{{baseUrl}}/api/produtores", "cer",
  {"nome": "CPF ruim", "email": "cpf-{{sufixo}}@teste.dev", "senha": "Teste@12345", "cpf": "11111111111"}, 400)
p(R, "200 - Buscar produtor", "GET", "{{baseUrl}}/api/produtores/{{novoProdutorId}}", "cer", None, 200)
p(R, "200 - Listar/pesquisar produtores", "GET", "{{baseUrl}}/api/produtores?busca=Postman", "cer", None, 200)
p(R, "200 - Atualizar produtor", "PUT", "{{baseUrl}}/api/produtores/{{novoProdutorId}}", "cer",
  {"nome": "Produtor Postman Atualizado", "email": "postman-{{sufixo}}@teste.dev", "telefone": "14999990000"}, 200)
p(R, "204 - Excluir (inativar) produtor sem saldo", "DELETE", "{{baseUrl}}/api/produtores/{{novoProdutorId}}", "cer", None, 204)
p(R, "404 - Produtor excluído", "GET", "{{baseUrl}}/api/produtores/{{novoProdutorId}}", "cer", None, 404)

E = "6. Entregas e requisições de venda - CRUD e regras de negócio"
p(E, "201 - Registrar entrega (credita saldo do produtor)", "POST", "{{baseUrl}}/api/entregas", "cer",
  {"produtorId": "{{produtorCarlosId}}", "produtoId": "{{produtoMilhoExternoId}}", "quantidade": 100,
   "observacao": "Entrega de teste Postman"}, 201, ("entregaId", "id"))
p(E, "400 - Validação: quantidade zero", "POST", "{{baseUrl}}/api/entregas", "cer",
  {"produtorId": "{{produtorCarlosId}}", "produtoId": "{{produtoMilhoExternoId}}", "quantidade": 0}, 400)
p(E, "200 - Buscar entrega", "GET", "{{baseUrl}}/api/entregas/{{entregaId}}", "pro", None, 200)
p(E, "200 - Atualizar entrega (ajusta o saldo)", "PUT", "{{baseUrl}}/api/entregas/{{entregaId}}", "cer",
  {"quantidade": 120, "observacao": "Ajustada para 120 sc"}, 200)
p(E, "200 - Listar entregas do produtor", "GET", "{{baseUrl}}/api/entregas?pagina=0&tamanho=5", "pro", None, 200)
p(E, "201 - Produtor solicita venda (reserva saldo)", "POST", "{{baseUrl}}/api/requisicoes", "pro",
  {"produtoId": "{{produtoMilhoExternoId}}", "quantidade": 50, "observacao": "Venda de teste Postman"}, 201,
  ("requisicaoId", "id"))
p(E, "422 - Regra: quantidade acima do saldo disponível", "POST", "{{baseUrl}}/api/requisicoes", "pro",
  {"produtoId": "{{produtoMilhoExternoId}}", "quantidade": 99999}, 422)
p(E, "200 - Produtor edita requisição pendente", "PUT", "{{baseUrl}}/api/requisicoes/{{requisicaoId}}", "pro",
  {"quantidade": 60, "observacao": "Aumentei para 60 sc"}, 200)
p(E, "403 - Cerealista não edita requisição do produtor", "PUT", "{{baseUrl}}/api/requisicoes/{{requisicaoId}}", "cer",
  {"quantidade": 10}, 403)
p(E, "409 - Regra: reduzir entrega abaixo do saldo reservado", "PUT", "{{baseUrl}}/api/entregas/{{entregaId}}", "cer",
  {"quantidade": 30}, 409)
p(E, "200 - Buscar requisição por id", "GET", "{{baseUrl}}/api/requisicoes/{{requisicaoId}}", "pro", None, 200)
p(E, "200 - Consulta personalizada: requisições por status", "GET", "{{baseUrl}}/api/requisicoes?status=PENDENTE",
  "cer", None, 200)
p(E, "204 - Produtor cancela (exclui) requisição pendente", "DELETE", "{{baseUrl}}/api/requisicoes/{{requisicaoId}}", "pro",
  None, 204)
p(E, "404 - Requisição excluída", "GET", "{{baseUrl}}/api/requisicoes/{{requisicaoId}}", "pro", None, 404)

F = "7. Funcionalidade do PI - fluxo de venda (solicitar, aprovar e gerar transação)"
p(F, "201 - Produtor solicita nova venda", "POST", "{{baseUrl}}/api/requisicoes", "pro",
  {"produtoId": "{{produtoMilhoExternoId}}", "quantidade": 40}, 201, ("vendaId", "id"))
p(F, "403 - Produtor não pode aprovar a própria venda", "POST", "{{baseUrl}}/api/requisicoes/{{vendaId}}/aprovar", "pro",
  None, 403)
p(F, "200 - Cerealista aprova: debita saldo e gera transação com descontos", "POST",
  "{{baseUrl}}/api/requisicoes/{{vendaId}}/aprovar", "cer", None, 200)
p(F, "409 - Regra: venda já aprovada não pode ser aprovada de novo", "POST",
  "{{baseUrl}}/api/requisicoes/{{vendaId}}/aprovar", "cer", None, 409)
p(F, "409 - Regra: venda aprovada não pode ser excluída", "DELETE", "{{baseUrl}}/api/requisicoes/{{vendaId}}", "pro", None, 409)
p(F, "200 - Cerealista processa a venda", "POST", "{{baseUrl}}/api/requisicoes/{{vendaId}}/processar", "cer", None, 200)
p(F, "200 - Produtor consulta suas transações (valor bruto, descontos e líquido)", "GET",
  "{{baseUrl}}/api/transacoes?pagina=0&tamanho=3", "pro", None, 200)
p(F, "200 - Produtor consulta seus saldos", "GET", "{{baseUrl}}/api/saldos", "pro", None, 200)

RUN["sufixo"] = str(int(time.time()))
RUN["cpfGerado"] = cpf_valido()
pastas = {}
for (pasta, nome, metodo, caminho, quem, corpo, esperado, guardar, pre) in PASSOS:
    status, motivo, texto, cab = chamar(metodo, caminho, quem, corpo)
    assert status == esperado, f"{nome}: esperado {esperado}, veio {status}: {texto[:300]}"
    if guardar and texto:
        RUN[guardar[0]] = pegar(json.loads(texto), guardar[1])
    try:
        corpo_resp = json.dumps(json.loads(texto), ensure_ascii=False, indent=2) if texto else ""
    except ValueError:
        corpo_resp = texto
    exec_url = subst(caminho)
    exec_corpo = subst(json.dumps(corpo, ensure_ascii=False, indent=2)) if corpo is not None else None

    def requisicao(url_tpl, corpo_json, quem_):
        header = [{"key": "Content-Type", "value": "application/json"}] if corpo_json else []
        req = {"method": metodo, "header": header,
               "url": {"raw": url_tpl,
                       "host": ["{{baseUrl}}"],
                       "path": url_tpl.replace("{{baseUrl}}/", "").split("?")[0].split("/"),
                       "query": [{"key": kv.split("=")[0], "value": kv.split("=")[1]}
                                 for kv in url_tpl.split("?")[1].split("&")] if "?" in url_tpl else []}}
        if corpo_json is not None:
            req["body"] = {"mode": "raw", "raw": corpo_json,
                           "options": {"raw": {"language": "json"}}}
        if quem_ == "cer":
            req["auth"] = {"type": "basic", "basic": [{"key": "username", "value": "{{cerealistaEmail}}", "type": "string"},
                                                      {"key": "password", "value": "{{cerealistaSenha}}", "type": "string"}]}
        elif quem_ == "pro":
            req["auth"] = {"type": "basic", "basic": [{"key": "username", "value": "{{produtorEmail}}", "type": "string"},
                                                      {"key": "password", "value": "{{produtorSenha}}", "type": "string"}]}
        elif quem_ == "errada":
            req["auth"] = {"type": "basic", "basic": [{"key": "username", "value": "{{cerealistaEmail}}", "type": "string"},
                                                      {"key": "password", "value": "senha-incorreta", "type": "string"}]}
        else:
            req["auth"] = {"type": "noauth"}
        return req

    corpo_tpl = json.dumps(corpo, ensure_ascii=False, indent=2) if corpo is not None else None
    req = requisicao(caminho, corpo_tpl, quem)
    req_exec = requisicao(exec_url, exec_corpo, quem)

    eventos = []
    if pre == "cadastro":
        eventos.append({"listen": "prerequest", "script": {"type": "text/javascript", "exec": [
            "pm.collectionVariables.set('sufixo', Date.now().toString());",
            "const d = Array.from({length: 9}, () => Math.floor(Math.random() * 10));",
            "if (new Set(d).size === 1) d[0] = (d[0] + 1) % 10;",
            "for (const j of [9, 10]) { let s = 0; for (let i = 0; i < j; i++) s += d[i] * (j + 1 - i); const r = (s * 10) % 11; d.push(r === 10 ? 0 : r); }",
            "pm.collectionVariables.set('cpfGerado', d.join(''));"]}})
    testes = [f"pm.test('Status {esperado}', function () {{ pm.response.to.have.status({esperado}); }});"]
    if guardar:
        testes.append(f"pm.collectionVariables.set('{guardar[0]}', pm.response.json().{guardar[1]});")
    eventos.append({"listen": "test", "script": {"type": "text/javascript", "exec": testes}})

    item = {"name": nome, "event": eventos, "request": req, "response": [{
        "name": f"{status} {motivo}", "originalRequest": req_exec, "status": motivo, "code": status,
        "_postman_previewlanguage": "json",
        "header": [{"key": "Content-Type", "value": cab.get("Content-Type", "application/json")}],
        "cookie": [], "body": corpo_resp}]}
    pastas.setdefault(pasta, []).append(item)

colecao = {
    "info": {
        "_postman_id": "5a7f0c1e-6b0a-4d1b-9a55-5afa11ce0001",
        "name": "SafraLink - Back-end (Projeto Integrador)",
        "description": "Coleção do back-end do SafraLink (Spring Boot + MySQL, HTTP Basic). Ordem de execução: pastas 1 a 7. "
                       "Pré-requisito: API em http://localhost:8080 com `safralink.dados-demo=true`. "
                       "As variáveis (usuários de teste, ids) ficam em Variables da coleção. Cada requisição tem uma resposta "
                       "salva (Save Response) como evidência.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"},
    "auth": {"type": "basic", "basic": [{"key": "username", "value": "{{cerealistaEmail}}", "type": "string"},
                                        {"key": "password", "value": "{{cerealistaSenha}}", "type": "string"}]},
    "item": [{"name": nome, "item": itens} for nome, itens in pastas.items()],
    "variable": [{"key": k, "value": v, "type": "string"} for k, v in VARS.items()] + [
        {"key": k, "value": "", "type": "string"} for k in (
            "produtoId", "precoId", "precoAtualizadoId", "descontoId", "novoProdutorId", "entregaId", "requisicaoId",
            "vendaId", "sufixo", "cpfGerado")],
}
destino = Path(__file__).with_name("projeto-integrador.postman_collection.json")
destino.write_text(json.dumps(colecao, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"OK: {sum(len(v) for v in pastas.values())} requisições em {len(pastas)} pastas -> {destino}")
