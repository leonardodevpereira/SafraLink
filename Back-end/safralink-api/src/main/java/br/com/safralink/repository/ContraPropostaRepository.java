package br.com.safralink.repository;

import br.com.safralink.domain.ContraProposta;
import br.com.safralink.domain.StatusContraProposta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContraPropostaRepository extends JpaRepository<ContraProposta, Long> {

    List<ContraProposta> findByRequisicaoIdOrderByCriadoEmAscIdAsc(Long requisicaoId);

    List<ContraProposta> findByRequisicaoIdAndStatus(Long requisicaoId, StatusContraProposta status);
}
