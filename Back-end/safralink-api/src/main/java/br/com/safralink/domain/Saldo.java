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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "saldos", uniqueConstraints = @UniqueConstraint(columnNames = {"produtor_id", "produto_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Saldo {
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
    private BigDecimal quantidade = BigDecimal.ZERO;

    public void creditar(BigDecimal valor) {
        quantidade = quantidade.add(valor);
    }

    public void debitar(BigDecimal valor) {
        quantidade = quantidade.subtract(valor);
    }
}
