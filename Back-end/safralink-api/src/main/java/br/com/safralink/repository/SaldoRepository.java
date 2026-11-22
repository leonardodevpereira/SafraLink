package br.com.safralink.repository;

import br.com.safralink.domain.Saldo;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaldoRepository extends JpaRepository<Saldo, Long> {

    List<Saldo> findByProdutorId(Long produtorId);

    Optional<Saldo> findByProdutorIdAndProdutoId(Long produtorId, Long produtoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Saldo s where s.produtor.id = :produtorId and s.produto.id = :produtoId")
    Optional<Saldo> buscarParaAtualizar(@Param("produtorId") Long produtorId, @Param("produtoId") Long produtoId);

    @Query("select s.produtor.id, sum(s.quantidade) from Saldo s group by s.produtor.id")
    List<Object[]> totaisPorProdutor();

    @Query("select coalesce(sum(s.quantidade), 0) from Saldo s where s.produtor.id = :produtorId")
    BigDecimal totalDoProdutor(@Param("produtorId") Long produtorId);

    boolean existsByProdutoId(Long produtoId);
}
