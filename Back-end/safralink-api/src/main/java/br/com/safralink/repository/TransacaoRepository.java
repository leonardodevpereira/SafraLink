package br.com.safralink.repository;

import br.com.safralink.domain.Transacao;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TransacaoRepository extends JpaRepository<Transacao, Long>, JpaSpecificationExecutor<Transacao> {
    Optional<Transacao> findByRequisicaoId(Long requisicaoId);

    List<Transacao> findByCriadoEmGreaterThanEqual(java.time.Instant desde);

    @Query("select coalesce(sum(t.requisicao.quantidade), 0) from Transacao t")
    BigDecimal totalSacas();

    @Query("select coalesce(sum(t.valorBruto), 0) from Transacao t")
    BigDecimal totalBruto();

    @Query("select coalesce(sum(t.totalDescontos), 0) from Transacao t")
    BigDecimal totalDescontos();

    @Query("select t.requisicao.produto.nome, sum(t.requisicao.quantidade), sum(t.valorBruto) "
            + "from Transacao t group by t.requisicao.produto.nome order by sum(t.valorBruto) desc")
    List<Object[]> comprasPorProduto();

    @Query("select t.requisicao.produtor.id, max(t.criadoEm) from Transacao t group by t.requisicao.produtor.id")
    List<Object[]> ultimaTransacaoPorProdutor();
}
