package br.com.safralink.repository;

import br.com.safralink.domain.DescontoAplicado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DescontoAplicadoRepository extends JpaRepository<DescontoAplicado, Long> {

    boolean existsByDescontoId(Long descontoId);
}
