package br.com.safralink.web.dto;

import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.TipoNotificacao;
import br.com.safralink.web.dto.CatalogoDtos.ProdutoResponse;
import br.com.safralink.web.dto.CatalogoDtos.SaldoResponse;
import br.com.safralink.web.dto.RequisicaoDtos.RequisicaoResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public final class OutrosDtos {
    private OutrosDtos() {}

    public record PageResponse<T>(List<T> conteudo, int pagina, int tamanho, long total, int totalPaginas) {
        public static <T> PageResponse<T> de(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }

    public record ProdutorResumoResponse(
            Long id,
            String nome,
            String cpf,
            String email,
            String telefone,
            String cidade,
            BigDecimal saldoTotalSacas,
            Instant ultimaTransacaoEm,
            boolean ativo) {}

    public record NotificacaoResponse(Long id, TipoNotificacao tipo, String mensagem, boolean lida, Instant criadoEm) {}

    public record AuditoriaResponse(
            Long id,
            String usuarioNome,
            String usuarioEmail,
            AcaoAuditoria acao,
            String entidade,
            Long entidadeId,
            String detalhes,
            Instant criadoEm) {}

    public record DashboardProdutorResponse(
            List<ProdutoResponse> precos,
            List<SaldoResponse> saldos,
            BigDecimal saldoTotalSacas,
            BigDecimal valorEstimadoTotal,
            long requisicoesPendentes,
            List<RequisicaoResponse> ultimasRequisicoes) {}

    public record CompraPorProduto(String produto, BigDecimal sacas, BigDecimal valor) {}

    public record CompraPorDia(LocalDate data, BigDecimal valor, BigDecimal sacas) {}

    public record DashboardCerealistaResponse(
            BigDecimal totalSacasCompradas,
            BigDecimal valorTotalComprado,
            BigDecimal totalDescontos,
            long produtoresAtivos,
            long requisicoesPendentes,
            List<CompraPorProduto> comprasPorProduto,
            List<CompraPorDia> comprasPorDia,
            List<RequisicaoResponse> pendentesRecentes) {}
}
