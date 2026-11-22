package br.com.safralink.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditorias")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nome", length = 150)
    private String usuarioNome;

    @Column(name = "usuario_email", length = 180)
    private String usuarioEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcaoAuditoria acao;

    @Column(length = 60)
    private String entidade;

    @Column(name = "entidade_id")
    private Long entidadeId;

    @Column(length = 500)
    private String detalhes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void antesDeSalvar() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }
}
