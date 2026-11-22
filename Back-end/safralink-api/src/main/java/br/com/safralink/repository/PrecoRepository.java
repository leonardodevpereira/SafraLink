package br.com.safralink.repository;

import br.com.safralink.domain.Preco;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrecoRepository extends JpaRepository<Preco, Long> {

    Optional<Preco> findFirstByProdutoIdOrderByCriadoEmDescIdDesc(Long produtoId);

    List<Preco> findTop2ByProdutoIdOrderByCriadoEmDescIdDesc(Long produtoId);

    List<Preco> findByProdutoIdAndCriadoEmGreaterThanEqualOrderByCriadoEmAscIdAsc(Long produtoId, Instant desde);

    boolean existsByProdutoId(Long produtoId);

    long countByProdutoId(Long produtoId);
}
