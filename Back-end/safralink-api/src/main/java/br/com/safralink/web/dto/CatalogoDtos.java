package br.com.safralink.web.dto;

import jakarta.validation.constraints.NotBlank;
import br.com.safralink.domain.Mercado;
import br.com.safralink.domain.TipoGrao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class CatalogoDtos {
    private CatalogoDtos() {}

    public record ProdutoResponse(
            Long id,
            String nome,
            TipoGrao grao,
            Mercado mercado,
            BigDecimal precoAtual,
            BigDecimal variacaoPercentual,
            Instant precoAtualizadoEm,
            BigDecimal quantidadeDisponivel) {}

    public record PrecoResponse(Long id, Long produtoId, String produtoNome, BigDecimal valorSaca, Instant criadoEm) {}

    public record PrecoRequest(
            @NotNull(message = "Informe o preço")
            @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais") BigDecimal valorSaca) {}

    public record QuantidadeResponse(Long produtoId, String produtoNome, BigDecimal quantidade, Instant atualizadoEm) {}

    public record QuantidadeRequest(
            @NotNull(message = "Informe a quantidade")
            @DecimalMin(value = "0.00", message = "A quantidade não pode ser negativa")
            @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais") BigDecimal quantidade) {}

    public record SaldoResponse(
            Long produtoId,
            String produtoNome,
            BigDecimal quantidade,
            BigDecimal reservado,
            BigDecimal disponivelParaVenda,
            BigDecimal precoAtual,
            BigDecimal valorEstimado) {}

    public record EntregaRequest(
            @NotNull(message = "Informe o produtor") Long produtorId,
            @NotNull(message = "Informe o produto") Long produtoId,
            @NotNull(message = "Informe a quantidade")
            @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais") BigDecimal quantidade,
            LocalDate dataEntrega,
            @Size(max = 255) String observacao) {}

    public record EntregaResponse(
            Long id,
            Long produtorId,
            String produtorNome,
            Long produtoId,
            String produtoNome,
            BigDecimal quantidade,
            LocalDate dataEntrega,
            String observacao,
            Instant criadoEm) {}

    public record ProdutoRequest(
            @NotBlank(message = "Informe o nome do produto") @Size(max = 60, message = "O nome pode ter no máximo 60 caracteres") String nome,
            @NotNull(message = "Informe o grão") TipoGrao grao,
            @NotNull(message = "Informe o mercado") Mercado mercado) {}

    public record PrecoCriarRequest(
            @NotNull(message = "Informe o produto") Long produtoId,
            @NotNull(message = "Informe o preço")
            @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais") BigDecimal valorSaca) {}

    public record EntregaAtualizacaoRequest(
            @NotNull(message = "Informe a quantidade")
            @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero")
            @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais") BigDecimal quantidade,
            LocalDate dataEntrega,
            @Size(max = 255) String observacao) {}
}
