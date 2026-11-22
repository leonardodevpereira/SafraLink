package br.com.safralink.web.dto;

import br.com.safralink.domain.CategoriaDesconto;
import br.com.safralink.domain.FormaDesconto;
import br.com.safralink.domain.Perfil;
import br.com.safralink.domain.StatusContraProposta;
import br.com.safralink.domain.StatusRequisicao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class RequisicaoDtos {
    private RequisicaoDtos() {}

    public record RequisicaoRequest(
            @NotNull(message = "Informe o produto") Long produtoId,
            @NotNull(message = "Informe a quantidade")
            @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais") BigDecimal quantidade,
            @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais") BigDecimal precoSaca,
            @Size(max = 255, message = "A observação pode ter no máximo 255 caracteres") String observacao) {
        public RequisicaoRequest(Long produtoId, BigDecimal quantidade, BigDecimal precoSaca) {
            this(produtoId, quantidade, precoSaca, null);
        }
    }

    public record RecusaRequest(@NotBlank(message = "Informe o motivo da recusa") @Size(max = 255) String motivo) {}

    public record ContraPropostaRequest(
            @NotNull(message = "Informe o preço proposto")
            @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais") BigDecimal precoSaca) {}

    public record ContraPropostaResponse(
            Long id,
            Long autorId,
            String autorNome,
            Perfil autorPerfil,
            BigDecimal precoSaca,
            StatusContraProposta status,
            Instant criadoEm,
            Instant respondidaEm) {}

    public record DescontoAplicadoResponse(String nome, BigDecimal valor) {}

    public record TransacaoResponse(
            Long id,
            Long requisicaoId,
            Long produtorId,
            String produtorNome,
            String produtorCpf,
            Long produtoId,
            String produtoNome,
            BigDecimal quantidade,
            BigDecimal precoSaca,
            BigDecimal valorBruto,
            BigDecimal totalDescontos,
            BigDecimal valorLiquido,
            StatusRequisicao status,
            List<DescontoAplicadoResponse> descontos,
            Instant criadoEm,
            Instant dataProcessamento,
            String compradoraNome,
            String compradoraDocumento,
            String compradoraEndereco,
            String compradoraCidade) {}

    public record RequisicaoResponse(
            Long id,
            Long produtorId,
            String produtorNome,
            Long produtoId,
            String produtoNome,
            BigDecimal quantidade,
            BigDecimal precoSaca,
            BigDecimal valorBruto,
            StatusRequisicao status,
            String motivoRecusa,
            String observacao,
            String aprovadoPorNome,
            Instant criadoEm,
            Instant atualizadoEm,
            List<ContraPropostaResponse> contraPropostas,
            TransacaoResponse transacao) {}

    public record DescontoRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 80) String nome,
            @NotNull(message = "Informe a categoria") CategoriaDesconto categoria,
            @NotNull(message = "Informe a forma de cálculo") FormaDesconto forma,
            @NotNull(message = "Informe o valor")
            @DecimalMin(value = "0.0", message = "O valor não pode ser negativo")
            @Digits(integer = 10, fraction = 4, message = "Use no máximo 4 casas decimais") BigDecimal valor,
            Boolean ativo) {}

    public record DescontoResponse(
            Long id, String nome, CategoriaDesconto categoria, FormaDesconto forma, BigDecimal valor, boolean ativo) {}

    public record RequisicaoAtualizacaoRequest(
            @NotNull(message = "Informe a quantidade")
            @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais") BigDecimal quantidade,
            @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais") BigDecimal precoSaca,
            @Size(max = 255, message = "A observação pode ter no máximo 255 caracteres") String observacao) {}
}
