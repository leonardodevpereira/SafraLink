package br.com.safralink.service;

import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.Usuario;
import br.com.safralink.repository.UsuarioRepository;
import br.com.safralink.web.ApiException;
import br.com.safralink.web.dto.UsuarioDtos.PerfilRequest;
import br.com.safralink.web.dto.UsuarioDtos.UsuarioResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository repository, AuditoriaService auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    public Usuario buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse meuPerfil(Long id) {
        return Mapeador.usuario(buscar(id));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, PerfilRequest req) {
        Usuario usuario = buscar(id);
        String email = req.email().trim().toLowerCase();
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflito("Este e-mail já está em uso por outra conta.");
        }
        usuario.setNome(req.nome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(Mapeador.limpar(req.telefone()));
        Mapeador.aplicarEndereco(usuario.getEndereco(), req.endereco());
        auditoria.registrar(usuario, AcaoAuditoria.PERFIL_ATUALIZADO, "usuarios", id, "Dados do perfil atualizados");
        return Mapeador.usuario(usuario);
    }
}
