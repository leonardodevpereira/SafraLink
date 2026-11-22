package br.com.safralink.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "descontos_aplicados")
@Getter
@Setter
@NoArgsConstructor
public class DescontoAplicado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transacao_id")
    private Transacao transacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "desconto_id")
    private Desconto desconto;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "valor_aplicado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorAplicado;
}
