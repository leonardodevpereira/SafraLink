package br.com.safralink.repository;

import br.com.safralink.domain.Produtor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutorRepository extends JpaRepository<Produtor, Long> {

    boolean existsByCpf(String cpf);

    List<Produtor> findByAtivoTrueOrderByNomeAsc();

    long countByAtivoTrue();
}
