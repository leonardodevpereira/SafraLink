package br.com.safralink.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transacoes")
@Getter
@Setter
@NoArgsConstructor
public class Transacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisicao_id", unique = true)
    private RequisicaoVenda requisicao;

    @Column(name = "valor_bruto", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "total_descontos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDescontos = BigDecimal.ZERO;

    @Column(name = "valor_liquido", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorLiquido;

    @Column(name = "data_processamento")
    private Instant dataProcessamento;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @OneToMany(mappedBy = "transacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DescontoAplicado> descontos = new ArrayList<>();

    @PrePersist
    void antesDeSalvar() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public void adicionarDesconto(DescontoAplicado d) {
        d.setTransacao(this);
        descontos.add(d);
    }
}
