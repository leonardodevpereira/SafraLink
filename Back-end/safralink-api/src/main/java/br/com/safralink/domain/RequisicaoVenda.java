package br.com.safralink.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "requisicoes_venda")
@Getter
@Setter
@NoArgsConstructor
public class RequisicaoVenda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produtor_id")
    private Produtor produtor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal quantidade;

    @Column(name = "preco_saca", nullable = false, precision = 14, scale = 2)
    private BigDecimal precoSaca;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRequisicao status = StatusRequisicao.PENDENTE;

    @Column(name = "motivo_recusa", length = 255)
    private String motivoRecusa;

    @Column(length = 255)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por_id")
    private Cerealista aprovadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void antesDeSalvar() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
        atualizadoEm = Instant.now();
    }

    @PreUpdate
    void antesDeAtualizar() {
        atualizadoEm = Instant.now();
    }

    public BigDecimal valorBruto() {
        return quantidade.multiply(precoSaca).setScale(2, RoundingMode.HALF_UP);
    }
}
