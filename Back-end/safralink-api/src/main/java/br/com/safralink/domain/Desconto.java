package br.com.safralink.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "descontos")
@Getter
@Setter
@NoArgsConstructor
public class Desconto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaDesconto categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaDesconto forma;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal valor;

    @Column(nullable = false)
    private boolean ativo = true;
}
