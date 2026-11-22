package br.com.safralink.repository;

import br.com.safralink.domain.Desconto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DescontoRepository extends JpaRepository<Desconto, Long> {

    List<Desconto> findByAtivoTrueOrderByIdAsc();

    List<Desconto> findAllByOrderByIdAsc();
}
