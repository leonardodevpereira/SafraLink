package br.com.safralink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import br.com.safralink.domain.Cerealista;
import br.com.safralink.repository.CerealistaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrudTest {
    private static final String SENHA = "Senha@12345";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CerealistaRepository cerealistaRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void limparProdutoDeTeste() {
        String filtro = "(select id from produtos where grao = 'FEIJAO' and mercado = 'EXTERNO')";
        String requisicoes = "(select id from requisicoes_venda where produto_id in " + filtro + ")";
        jdbc.update("delete from descontos_aplicados where transacao_id in "
                + "(select id from transacoes where requisicao_id in " + requisicoes + ")");
        jdbc.update("delete from transacoes where requisicao_id in " + requisicoes);
        jdbc.update("delete from contra_propostas where requisicao_id in " + requisicoes);
        jdbc.update("delete from requisicoes_venda where produto_id in " + filtro);
        for (String tabela : new String[] {"precos", "quantidades_disponiveis", "saldos", "entregas"}) {
            jdbc.update("delete from " + tabela + " where produto_id in " + filtro);
        }
        jdbc.update("delete from produtos where grao = 'FEIJAO' and mercado = 'EXTERNO'");
    }

    @Test
    void crudCompletoComRegrasEAutorizacao() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailC = "crud-c-" + sufixo + "@teste.dev";
        Cerealista c = new Cerealista();
        c.setNome("Cerealista CRUD");
        c.setRazaoSocial("Cerealista CRUD Ltda");
        c.setCnpj(String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000_000_000L, 99_999_999_999_999L)));
        c.setEmail(emailC);
        c.setSenhaHash(encoder.encode(SENHA));
        cerealistaRepository.save(c);
        String cer = basic(emailC);

        chamar(get("/api/produtos"), null, null, 401);
        chamar(get("/api/produtos"), basic64(emailC, "errada"), null, 401);

        String emailP = "crud-p-" + sufixo + "@teste.dev";
        JsonNode prod = chamar(post("/api/produtores"), cer, Map.of("nome", "Produtor CRUD", "email", emailP,
                "senha", SENHA, "cpf", cpf(sufixo)), 201);
        long produtorId = prod.get("id").asLong();
        String pro = basic(emailP);
        chamar(post("/api/produtos"), pro, Map.of("nome", "X", "grao", "SOJA", "mercado", "INTERNO"), 403);
        chamar(get("/api/produtores"), pro, null, 403);

        JsonNode feijaoExterno = chamar(post("/api/produtos"), cer,
                Map.of("nome", "Feijão Externo " + sufixo, "grao", "FEIJAO", "mercado", "EXTERNO"), 201);
        long produtoId = feijaoExterno.get("id").asLong();
        chamar(post("/api/produtos"), cer, Map.of("nome", "Dup", "grao", "FEIJAO", "mercado", "EXTERNO"), 409);
        chamar(post("/api/produtos"), cer, Map.of("nome", "", "grao", "FEIJAO", "mercado", "EXTERNO"), 400);
        assertThat(chamar(get("/api/produtos/" + produtoId), pro, null, 200).get("nome").asText()).contains("Feijão");
        chamar(put("/api/produtos/" + produtoId), cer,
                Map.of("nome", "Feijão Ext. " + sufixo, "grao", "FEIJAO", "mercado", "EXTERNO"), 200);
        chamar(get("/api/produtos/999999"), cer, null, 404);
        assertThat(chamar(get("/api/produtos?grao=FEIJAO"), cer, null, 200).size()).isGreaterThanOrEqualTo(1);
        assertThat(chamar(get("/api/quantidades?abaixoDe=1"), cer, null, 200).size()).isGreaterThanOrEqualTo(1);

        long preco1 = chamar(post("/api/precos"), cer, Map.of("produtoId", produtoId, "valorSaca", 200), 201).get("id").asLong();
        chamar(get("/api/precos/" + preco1), pro, null, 200);
        chamar(delete("/api/precos/" + preco1), cer, null, 409);
        long preco2 = chamar(post("/api/precos"), cer, Map.of("produtoId", produtoId, "valorSaca", 210), 201).get("id").asLong();
        chamar(delete("/api/precos/" + preco2), cer, null, 204);
        chamar(get("/api/precos/" + preco2), cer, null, 404);

        chamar(delete("/api/produtos/" + produtoId), cer, null, 409);

        long descontoId = chamar(post("/api/descontos"), cer, Map.of("nome", "Teste " + sufixo, "categoria", "ARMAZENAGEM",
                "forma", "PERCENTUAL", "valor", 1.5), 201).get("id").asLong();
        chamar(get("/api/descontos/" + descontoId), pro, null, 200);
        chamar(delete("/api/descontos/" + descontoId), pro, null, 403);
        chamar(delete("/api/descontos/" + descontoId), cer, null, 204);
        chamar(get("/api/descontos/" + descontoId), cer, null, 404);

        chamar(put("/api/quantidades/" + produtoId), cer, Map.of("quantidade", 1000), 200);
        long entregaId = chamar(post("/api/entregas"), cer, Map.of("produtorId", produtorId, "produtoId", produtoId,
                "quantidade", 100), 201).get("id").asLong();
        chamar(put("/api/entregas/" + entregaId), cer, Map.of("quantidade", 120, "observacao", "ajustada"), 200);
        assertThat(saldo(pro, produtoId).get("quantidade").decimalValue()).isEqualByComparingTo("120");
        chamar(get("/api/entregas/" + entregaId), pro, null, 200);
        chamar(put("/api/entregas/" + entregaId), pro, Map.of("quantidade", 1), 403);

        long reqId = chamar(post("/api/requisicoes"), pro, Map.of("produtoId", produtoId, "quantidade", 50), 201)
                .get("id").asLong();
        chamar(put("/api/requisicoes/" + reqId), pro, Map.of("quantidade", 60, "observacao", "mais"), 200);
        chamar(put("/api/requisicoes/" + reqId), pro, Map.of("quantidade", 500), 422);
        chamar(put("/api/requisicoes/" + reqId), pro, Map.of("quantidade", 0), 400);
        chamar(put("/api/requisicoes/" + reqId), cer, Map.of("quantidade", 10), 403);
        chamar(put("/api/entregas/" + entregaId), cer, Map.of("quantidade", 30), 409);
        chamar(delete("/api/entregas/" + entregaId), cer, null, 409);
        chamar(delete("/api/requisicoes/" + reqId), pro, null, 204);
        chamar(get("/api/requisicoes/" + reqId), pro, null, 404);

        chamar(delete("/api/entregas/" + entregaId), cer, null, 204);
        chamar(get("/api/entregas/" + entregaId), cer, null, 404);
        assertThat(saldo(pro, produtoId).get("quantidade").decimalValue()).isEqualByComparingTo("0");

        chamar(get("/api/produtores/" + produtorId), cer, null, 200);
        chamar(put("/api/produtores/" + produtorId), cer,
                Map.of("nome", "Produtor Atualizado", "email", emailP, "telefone", "14999990000"), 200);
        chamar(put("/api/produtores/" + produtorId), cer, Map.of("nome", "", "email", "invalido"), 400);
        chamar(delete("/api/produtores/" + produtorId), cer, null, 204);
        chamar(get("/api/produtores/" + produtorId), cer, null, 404);
        chamar(get("/api/usuarios/me"), pro, null, 401);
    }

    @Test
    void crudDasEntidadesDerivadas() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailC = "der-c-" + sufixo + "@teste.dev";
        Cerealista c = new Cerealista();
        c.setNome("Cerealista Derivadas");
        c.setRazaoSocial("Cerealista Derivadas Ltda");
        c.setCnpj(String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000_000_000L, 99_999_999_999_999L)));
        c.setEmail(emailC);
        c.setSenhaHash(encoder.encode(SENHA));
        cerealistaRepository.save(c);
        String cer = basic(emailC);

        String emailP = "der-p-" + sufixo + "@teste.dev";
        long produtorId = chamar(post("/api/produtores"), cer, Map.of("nome", "Produtor Derivadas", "email", emailP,
                "senha", SENHA, "cpf", cpf(sufixo)), 201).get("id").asLong();
        String pro = basic(emailP);

        long produtoId = chamar(post("/api/produtos"), cer,
                Map.of("nome", "Feijão Derivadas " + sufixo, "grao", "FEIJAO", "mercado", "EXTERNO"), 201)
                .get("id").asLong();
        chamar(post("/api/precos"), cer, Map.of("produtoId", produtoId, "valorSaca", 200), 201);
        chamar(put("/api/quantidades/" + produtoId), cer, Map.of("quantidade", 1000), 200);
        chamar(post("/api/entregas"), cer, Map.of("produtorId", produtorId, "produtoId", produtoId,
                "quantidade", 100), 201);
        long reqId = chamar(post("/api/requisicoes"), pro, Map.of("produtoId", produtoId, "quantidade", 50), 201)
                .get("id").asLong();

        long cpId = chamar(post("/api/requisicoes/" + reqId + "/contra-propostas"), cer, Map.of("precoSaca", 190), 201)
                .get("contraPropostas").get(0).get("id").asLong();
        assertThat(chamar(get("/api/requisicoes/" + reqId + "/contra-propostas"), pro, null, 200).size()).isEqualTo(1);
        assertThat(chamar(get("/api/contra-propostas/" + cpId), pro, null, 200).get("precoSaca").decimalValue())
                .isEqualByComparingTo("190");
        chamar(get("/api/contra-propostas/999999999"), cer, null, 404);
        chamar(put("/api/contra-propostas/" + cpId), pro, Map.of("precoSaca", 150), 403);
        chamar(put("/api/contra-propostas/" + cpId), cer, Map.of("precoSaca", 0), 400);
        chamar(put("/api/contra-propostas/" + cpId), cer, Map.of("precoSaca", 195), 200);
        assertThat(chamar(get("/api/contra-propostas/" + cpId), cer, null, 200).get("precoSaca").decimalValue())
                .isEqualByComparingTo("195");
        chamar(delete("/api/contra-propostas/" + cpId), pro, null, 403);
        chamar(delete("/api/contra-propostas/" + cpId), cer, null, 204);
        chamar(get("/api/contra-propostas/" + cpId), cer, null, 404);

        long cpAceita = chamar(post("/api/requisicoes/" + reqId + "/contra-propostas"), cer, Map.of("precoSaca", 198), 201)
                .get("contraPropostas").get(0).get("id").asLong();
        chamar(post("/api/contra-propostas/" + cpAceita + "/aceitar"), pro, null, 200);
        chamar(put("/api/contra-propostas/" + cpAceita), cer, Map.of("precoSaca", 100), 409);
        chamar(delete("/api/contra-propostas/" + cpAceita), cer, null, 409);

        JsonNode notificacoes = chamar(get("/api/notificacoes"), pro, null, 200);
        assertThat(notificacoes.size()).isGreaterThanOrEqualTo(1);
        long notificacaoId = notificacoes.get(0).get("id").asLong();
        chamar(get("/api/notificacoes/" + notificacaoId), pro, null, 200);
        chamar(get("/api/notificacoes/" + notificacaoId), cer, null, 404);
        chamar(delete("/api/notificacoes/" + notificacaoId), cer, null, 404);
        chamar(delete("/api/notificacoes/" + notificacaoId), pro, null, 204);
        chamar(get("/api/notificacoes/" + notificacaoId), pro, null, 404);

        assertThat(chamar(get("/api/saldos/" + produtoId), pro, null, 200).get("quantidade").decimalValue())
                .isEqualByComparingTo("100");
        chamar(get("/api/saldos/" + produtoId), cer, null, 403);
        chamar(get("/api/saldos/999999999"), pro, null, 404);

        JsonNode aprovada = chamar(post("/api/requisicoes/" + reqId + "/aprovar"), cer, null, 200);
        long transacaoId = aprovada.get("transacao").get("id").asLong();
        chamar(get("/api/transacoes/" + transacaoId), pro, null, 200);
        chamar(get("/api/transacoes/" + transacaoId), cer, null, 200);
        chamar(get("/api/transacoes/999999999"), cer, null, 404);

        long auditoriaId = chamar(get("/api/auditoria?tamanho=1"), cer, null, 200).get("conteudo").get(0).get("id").asLong();
        chamar(get("/api/auditoria/" + auditoriaId), cer, null, 200);
        chamar(get("/api/auditoria/" + auditoriaId), pro, null, 403);
        chamar(get("/api/auditoria/999999999"), cer, null, 404);
    }

    private JsonNode saldo(String cred, long produtoId) throws Exception {
        for (JsonNode s : chamar(get("/api/saldos"), cred, null, 200)) {
            if (s.get("produtoId").asLong() == produtoId) {
                return s;
            }
        }
        throw new AssertionError("Saldo não encontrado");
    }

    private static String basic(String email) {
        return basic64(email, SENHA);
    }

    private static String basic64(String email, String senha) {
        return Base64.getEncoder().encodeToString((email + ":" + senha).getBytes());
    }

    private JsonNode chamar(MockHttpServletRequestBuilder req, String cred, Object corpo, int esperado) throws Exception {
        if (cred != null) {
            req.header("Authorization", "Basic " + cred);
        }
        if (corpo != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
        }
        var resposta = mvc.perform(req).andReturn().getResponse();
        String texto = resposta.getContentAsString();
        assertThat(resposta.getStatus()).as("corpo da resposta: %s", texto).isEqualTo(esperado);
        return texto.isBlank() ? json.createObjectNode() : json.readTree(texto);
    }

    private static String cpf(String sufixo) {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) {
            d[i] = ThreadLocalRandom.current().nextInt(10);
        }
        if (java.util.Arrays.stream(d, 0, 9).distinct().count() == 1) {
            d[0] = (d[0] + 1) % 10;
        }
        for (int j = 9; j < 11; j++) {
            int soma = 0;
            for (int i = 0; i < j; i++) {
                soma += d[i] * (j + 1 - i);
            }
            int r = (soma * 10) % 11;
            d[j] = r == 10 ? 0 : r;
        }
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }
}
