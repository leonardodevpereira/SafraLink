package br.com.safralink.repository;

import br.com.safralink.domain.Cerealista;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CerealistaRepository extends JpaRepository<Cerealista, Long> {

    Optional<Cerealista> findFirstByOrderByIdAsc();

    boolean existsByCnpj(String cnpj);
}
