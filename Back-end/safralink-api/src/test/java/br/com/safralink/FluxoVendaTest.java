package br.com.safralink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.safralink.domain.Cerealista;
import br.com.safralink.repository.CerealistaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoVendaTest {
    private static final String SENHA = "Senha@12345";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CerealistaRepository cerealistaRepository;
    @Autowired PasswordEncoder encoder;

    @Test
    void fluxoCompletoDeVenda() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        String emailCerealista = "cerealista-" + sufixo + "@teste.dev";
        Cerealista c = new Cerealista();
        c.setNome("Cerealista Teste");
        c.setRazaoSocial("Cerealista Teste Ltda");
        c.setCnpj(cnpjAleatorio());
        c.setEmail(emailCerealista);
        c.setSenhaHash(encoder.encode(SENHA));
        cerealistaRepository.save(c);
        String tokenCerealista = login(emailCerealista, "CEREALISTA");

        JsonNode reg = chamar(post("/api/auth/registro"), null, Map.of(
                "nome", "Produtor Um", "email", "p1-" + sufixo + "@teste.dev", "senha", SENHA,
                "cpf", cpfAleatorio()), 201);
        String tokenP1 = basic(reg.get("email").asText());
        long produtor1 = reg.get("id").asLong();
        assertThat(reg.get("perfil").asText()).isEqualTo("PRODUTOR");
        String tokenP2 = chamar(post("/api/auth/registro"), null, Map.of(
                "nome", "Produtor Dois", "email", "p2-" + sufixo + "@teste.dev", "senha", SENHA,
                "cpf", cpfAleatorio()), 201).get("email").asText();
        tokenP2 = basic(tokenP2);

        chamar(post("/api/auth/registro"), null, Map.of("nome", "Dup", "email", "p1-" + sufixo + "@teste.dev",
                "senha", SENHA, "cpf", cpfAleatorio()), 409);
        chamar(post("/api/auth/registro"), null, Map.of("nome", "X", "email", "x-" + sufixo + "@teste.dev",
                "senha", SENHA, "cpf", "111.111.111-11"), 400);
        chamar(post("/api/auth/registro"), null, Map.of("nome", "X", "email", "y-" + sufixo + "@teste.dev",
                "senha", "curta", "cpf", cpfAleatorio()), 400);

        mvc.perform(get("/api/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Basic "
                + java.util.Base64.getEncoder().encodeToString((emailCerealista + ":errada").getBytes())))
                .andExpect(status().isUnauthorized());
        chamar(get("/api/usuarios/me"), tokenCerealista, null, 200);

        long soja = produtoId(tokenCerealista, "Soja Interno");
        chamar(put("/api/precos/" + soja), tokenCerealista, Map.of("valorSaca", 100.00), 200);
        chamar(put("/api/quantidades/" + soja), tokenCerealista, Map.of("quantidade", 500), 200);
        for (JsonNode d : chamar(get("/api/descontos"), tokenCerealista, null, 200)) {
            chamar(put("/api/descontos/" + d.get("id").asLong()), tokenCerealista, Map.of("nome", d.get("nome").asText(),
                    "categoria", d.get("categoria").asText(), "forma", d.get("forma").asText(),
                    "valor", d.get("valor").decimalValue(), "ativo", false), 200);
        }
        chamar(post("/api/descontos"), tokenCerealista, Map.of("nome", "Armazenagem", "categoria", "ARMAZENAGEM",
                "forma", "PERCENTUAL", "valor", 10, "ativo", true), 201);

        chamar(put("/api/precos/" + soja), tokenP1, Map.of("valorSaca", 1.00), 403);
        chamar(get("/api/produtores"), tokenP1, null, 403);

        chamar(post("/api/requisicoes"), tokenP1, Map.of("produtoId", soja, "quantidade", 10), 422);

        chamar(post("/api/entregas"), tokenCerealista, Map.of("produtorId", produtor1, "produtoId", soja,
                "quantidade", 100), 201);
        assertThat(saldo(tokenP1, soja).get("quantidade").decimalValue()).isEqualByComparingTo("100");

        JsonNode req = chamar(post("/api/requisicoes"), tokenP1,
                Map.of("produtoId", soja, "quantidade", 40, "observacao", "  Posso entregar na quinta.  "), 201);
        long reqId = req.get("id").asLong();
        assertThat(req.get("observacao").asText()).isEqualTo("Posso entregar na quinta.");
        chamar(post("/api/requisicoes"), tokenP1,
                Map.of("produtoId", soja, "quantidade", 1, "observacao", "x".repeat(256)), 400);
        assertThat(req.get("status").asText()).isEqualTo("PENDENTE");
        assertThat(req.get("precoSaca").decimalValue()).isEqualByComparingTo("100");
        chamar(post("/api/requisicoes"), tokenP1, Map.of("produtoId", soja, "quantidade", 70), 422);
        assertThat(saldo(tokenP1, soja).get("disponivelParaVenda").decimalValue()).isEqualByComparingTo("60");

        chamar(get("/api/requisicoes/" + reqId), tokenP2, null, 404);

        JsonNode comProposta = chamar(post("/api/requisicoes/" + reqId + "/contra-propostas"), tokenP1,
                Map.of("precoSaca", 110.00), 201);
        long cpId = comProposta.get("contraPropostas").get(0).get("id").asLong();
        chamar(post("/api/contra-propostas/" + cpId + "/aceitar"), tokenP1, null, 403);
        JsonNode aceita = chamar(post("/api/contra-propostas/" + cpId + "/aceitar"), tokenCerealista, null, 200);
        assertThat(aceita.get("precoSaca").decimalValue()).isEqualByComparingTo("110");

        chamar(post("/api/requisicoes/" + reqId + "/aprovar"), tokenP1, null, 403);
        JsonNode aprovada = chamar(post("/api/requisicoes/" + reqId + "/aprovar"), tokenCerealista, null, 200);
        assertThat(aprovada.get("status").asText()).isEqualTo("APROVADA");
        JsonNode transacao = aprovada.get("transacao");
        assertThat(transacao.get("valorBruto").decimalValue()).isEqualByComparingTo("4400.00");
        assertThat(transacao.get("totalDescontos").decimalValue()).isEqualByComparingTo("440.00");
        assertThat(transacao.get("valorLiquido").decimalValue()).isEqualByComparingTo("3960.00");
        assertThat(transacao.get("compradoraNome").asText()).isEqualTo("Cerealista Teste Ltda");
        assertThat(transacao.get("compradoraDocumento").asText()).hasSize(14);
        assertThat(saldo(tokenP1, soja).get("quantidade").decimalValue()).isEqualByComparingTo("60");
        chamar(post("/api/requisicoes/" + reqId + "/aprovar"), tokenCerealista, null, 409);

        BigDecimal disponivel = null;
        for (JsonNode p : chamar(get("/api/produtos"), tokenP1, null, 200)) {
            if (p.get("id").asLong() == soja) {
                disponivel = p.get("quantidadeDisponivel").decimalValue();
            }
        }
        assertThat(disponivel).isEqualByComparingTo("460");

        JsonNode processada = chamar(post("/api/requisicoes/" + reqId + "/processar"), tokenCerealista, null, 200);
        assertThat(processada.get("status").asText()).isEqualTo("PROCESSADA");
        assertThat(processada.get("transacao").get("dataProcessamento")).isNotNull();

        long segunda = chamar(post("/api/requisicoes"), tokenP1, Map.of("produtoId", soja, "quantidade", 10), 201)
                .get("id").asLong();
        chamar(post("/api/requisicoes/" + segunda + "/recusar"), tokenCerealista, Map.of("motivo", ""), 400);
        JsonNode recusada = chamar(post("/api/requisicoes/" + segunda + "/recusar"), tokenCerealista,
                Map.of("motivo", "Preço fora do mercado"), 200);
        assertThat(recusada.get("status").asText()).isEqualTo("RECUSADA");
        JsonNode reenviada = chamar(post("/api/requisicoes/" + segunda + "/reenviar"), tokenP1, null, 200);
        assertThat(reenviada.get("status").asText()).isEqualTo("PENDENTE");
        assertThat(reenviada.has("motivoRecusa")).isFalse();

        chamar(put("/api/quantidades/" + soja), tokenCerealista, Map.of("quantidade", 5), 200);
        chamar(post("/api/requisicoes/" + segunda + "/aprovar"), tokenCerealista, null, 422);

        JsonNode lista = chamar(get("/api/requisicoes"), tokenP1, null, 200);
        assertThat(lista.get("total").asLong()).isEqualTo(2);
        JsonNode transacoes = chamar(get("/api/transacoes"), tokenP1, null, 200);
        assertThat(transacoes.get("total").asLong()).isEqualTo(1);
        assertThat(chamar(get("/api/notificacoes/nao-lidas"), tokenP1, null, 200).size()).isGreaterThanOrEqualTo(3);
        chamar(post("/api/notificacoes/lidas"), tokenP1, null, 204);
        assertThat(chamar(get("/api/notificacoes/nao-lidas"), tokenP1, null, 200).size()).isZero();
        assertThat(chamar(get("/api/notificacoes"), tokenP1, null, 200).size()).isGreaterThanOrEqualTo(3);
        chamar(delete("/api/notificacoes"), tokenP1, null, 204);
        assertThat(chamar(get("/api/notificacoes"), tokenP1, null, 200).size()).isZero();
        assertThat(chamar(get("/api/notificacoes"), tokenCerealista, null, 200).size()).isGreaterThanOrEqualTo(1);

        JsonNode auditoria = chamar(get("/api/auditoria?acao=APROVACAO"), tokenCerealista, null, 200);
        assertThat(auditoria.get("total").asLong()).isGreaterThanOrEqualTo(1);
        chamar(get("/api/auditoria"), tokenP1, null, 403);

        String csv = mvc.perform(get("/api/relatorios/transacoes.csv").header("Authorization", "Basic " + tokenCerealista))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(csv).contains("Produtor Um").contains("3960,00");

        JsonNode dash = chamar(get("/api/dashboard/cerealista"), tokenCerealista, null, 200);
        assertThat(dash.get("requisicoesPendentes").asLong()).isGreaterThanOrEqualTo(1);
        JsonNode dashP = chamar(get("/api/dashboard/produtor"), tokenP1, null, 200);
        assertThat(dashP.get("saldoTotalSacas").decimalValue()).isEqualByComparingTo("60");
    }

    private String login(String email, String perfil) {
        return basic(email);
    }

    private static String basic(String email) {
        return java.util.Base64.getEncoder().encodeToString((email + ":" + SENHA).getBytes());
    }

    private long produtoId(String token, String nome) throws Exception {
        for (JsonNode p : chamar(get("/api/produtos"), token, null, 200)) {
            if (nome.equals(p.get("nome").asText())) {
                return p.get("id").asLong();
            }
        }
        throw new AssertionError("Produto não encontrado: " + nome);
    }

    private JsonNode saldo(String token, long produtoId) throws Exception {
        for (JsonNode s : chamar(get("/api/saldos"), token, null, 200)) {
            if (s.get("produtoId").asLong() == produtoId) {
                return s;
            }
        }
        throw new AssertionError("Saldo não encontrado");
    }

    private JsonNode chamar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
                            String token, Object corpo, int statusEsperado) throws Exception {
        if (token != null) {
            req.header("Authorization", "Basic " + token);
        }
        if (corpo != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
        }
        var resposta = mvc.perform(req).andReturn().getResponse();
        String texto = resposta.getContentAsString();
        assertThat(resposta.getStatus()).as("corpo da resposta: %s", texto).isEqualTo(statusEsperado);
        return texto.isBlank() ? json.createObjectNode() : json.readTree(texto);
    }

    private static String cpfAleatorio() {
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
            int resto = (soma * 10) % 11;
            d[j] = resto == 10 ? 0 : resto;
        }
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }

    private static String cnpjAleatorio() {
        int[] d = new int[14];
        for (int i = 0; i < 12; i++) {
            d[i] = ThreadLocalRandom.current().nextInt(10);
        }
        int[] p1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] p2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int s = 0;
        for (int i = 0; i < 12; i++) {
            s += d[i] * p1[i];
        }
        d[12] = s % 11 < 2 ? 0 : 11 - s % 11;
        s = 0;
        for (int i = 0; i < 13; i++) {
            s += d[i] * p2[i];
        }
        d[13] = s % 11 < 2 ? 0 : 11 - s % 11;
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }
}
