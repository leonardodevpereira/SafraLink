package br.com.safralink.repository;

import java.math.BigDecimal;
import java.util.List;
import br.com.safralink.domain.QuantidadeDisponivel;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuantidadeDisponivelRepository extends JpaRepository<QuantidadeDisponivel, Long> {
    Optional<QuantidadeDisponivel> findByProdutoId(Long produtoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QuantidadeDisponivel q where q.produto.id = :produtoId")
    Optional<QuantidadeDisponivel> buscarParaAtualizar(@Param("produtoId") Long produtoId);

    @Query("select q from QuantidadeDisponivel q where q.quantidade < :limite order by q.quantidade asc")
    List<QuantidadeDisponivel> abaixoDe(@Param("limite") BigDecimal limite);
}
