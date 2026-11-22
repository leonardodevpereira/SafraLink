package br.com.safralink.service;

import br.com.safralink.security.UsuarioPrincipal;
import br.com.safralink.util.Tempo;
import br.com.safralink.web.dto.OutrosDtos.ProdutorResumoResponse;
import br.com.safralink.web.dto.RequisicaoDtos.TransacaoResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatorioService {
    private static final String BOM = "﻿";
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(Tempo.ZONA);

    private final TransacaoService transacaoService;
    private final ProdutorService produtorService;

    public RelatorioService(TransacaoService transacaoService, ProdutorService produtorService) {
        this.transacaoService = transacaoService;
        this.produtorService = produtorService;
    }

    @Transactional(readOnly = true)
    public byte[] transacoes(UsuarioPrincipal usuario, LocalDate de, LocalDate ate) {
        List<TransacaoResponse> itens = transacaoService.listarParaRelatorio(usuario, de, ate);
        StringBuilder csv = new StringBuilder(BOM);
        csv.append(linha("Data", "Produtor", "CPF", "Produto", "Sacas", "Preço/sc (R$)", "Valor bruto (R$)",
                "Descontos (R$)", "Valor líquido (R$)", "Situação", "Descontos aplicados"));
        for (TransacaoResponse t : itens) {
            String descontos = t.descontos().stream().map(d -> d.nome() + " " + numero(d.valor()))
                    .collect(Collectors.joining(" + "));
            csv.append(linha(data(t.criadoEm()), t.produtorNome(), t.produtorCpf(), t.produtoNome(),
                    numero(t.quantidade()), numero(t.precoSaca()), numero(t.valorBruto()),
                    numero(t.totalDescontos()), numero(t.valorLiquido()), t.status().name(), descontos));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] produtores() {
        StringBuilder csv = new StringBuilder(BOM);
        csv.append(linha("Produtor", "CPF", "E-mail", "Telefone", "Cidade", "Saldo total (sc)",
                "Última transação"));
        for (ProdutorResumoResponse p : produtorService.listar(null)) {
            csv.append(linha(p.nome(), p.cpf(), p.email(), p.telefone(), p.cidade(), numero(p.saldoTotalSacas()),
                    data(p.ultimaTransacaoEm())));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String linha(String... colunas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < colunas.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(escapar(colunas[i]));
        }
        return sb.append("\r\n").toString();
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        String v = valor;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !v.matches("-?[0-9]+(,[0-9]+)?")) {
            v = "'" + v;
        }
        if (v.contains(";") || v.contains("\"") || v.contains("\n")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static String numero(BigDecimal valor) {
        return valor == null ? "" : valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String data(Instant instante) {
        return instante == null ? "" : DATA_HORA.format(instante);
    }
}
