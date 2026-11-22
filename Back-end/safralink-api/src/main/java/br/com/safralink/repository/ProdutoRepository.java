package br.com.safralink.repository;

import br.com.safralink.domain.Mercado;
import br.com.safralink.domain.TipoGrao;
import br.com.safralink.domain.Produto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrueOrderByIdAsc();

    List<Produto> findByGraoAndAtivoTrueOrderByIdAsc(TipoGrao grao);

    boolean existsByGraoAndMercado(TipoGrao grao, Mercado mercado);

    boolean existsByGraoAndMercadoAndIdNot(TipoGrao grao, Mercado mercado, Long id);
}
