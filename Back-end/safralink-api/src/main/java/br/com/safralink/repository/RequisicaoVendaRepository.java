package br.com.safralink.repository;

import br.com.safralink.domain.RequisicaoVenda;
import br.com.safralink.domain.StatusRequisicao;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequisicaoVendaRepository
        extends JpaRepository<RequisicaoVenda, Long>, JpaSpecificationExecutor<RequisicaoVenda> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RequisicaoVenda r where r.id = :id")
    Optional<RequisicaoVenda> buscarParaAtualizar(@Param("id") Long id);

    @Query("select coalesce(sum(r.quantidade), 0) from RequisicaoVenda r "
            + "where r.produtor.id = :produtorId and r.produto.id = :produtoId and r.status = :status")
    BigDecimal somarQuantidade(@Param("produtorId") Long produtorId, @Param("produtoId") Long produtoId,
                               @Param("status") StatusRequisicao status);

    long countByStatus(StatusRequisicao status);

    long countByProdutorIdAndStatus(Long produtorId, StatusRequisicao status);

    List<RequisicaoVenda> findTop5ByProdutorIdOrderByCriadoEmDesc(Long produtorId);

    List<RequisicaoVenda> findTop5ByStatusOrderByCriadoEmDesc(StatusRequisicao status);

    boolean existsByProdutoId(Long produtoId);

    boolean existsByProdutorIdAndStatus(Long produtorId, StatusRequisicao status);
}
