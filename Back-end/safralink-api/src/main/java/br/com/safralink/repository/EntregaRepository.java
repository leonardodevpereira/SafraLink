package br.com.safralink.repository;

import br.com.safralink.domain.Entrega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    Page<Entrega> findByProdutorIdOrderByDataEntregaDescIdDesc(Long produtorId, Pageable pageable);

    Page<Entrega> findAllByOrderByDataEntregaDescIdDesc(Pageable pageable);

    boolean existsByProdutoId(Long produtoId);
}
