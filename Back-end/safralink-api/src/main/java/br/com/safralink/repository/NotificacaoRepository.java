package br.com.safralink.repository;

import br.com.safralink.domain.Notificacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findTop50ByUsuarioIdOrderByCriadoEmDescIdDesc(Long usuarioId);

    List<Notificacao> findByUsuarioIdAndLidaFalseOrderByCriadoEmAscIdAsc(Long usuarioId);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.usuario.id = :usuarioId and n.lida = false")
    int marcarTodasComoLidas(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("delete from Notificacao n where n.usuario.id = :usuarioId")
    int apagarTodas(@Param("usuarioId") Long usuarioId);
}
