package br.com.safralink.service;

import br.com.safralink.domain.AcaoAuditoria;
import br.com.safralink.domain.Cerealista;
import br.com.safralink.domain.Produtor;
import br.com.safralink.domain.TipoNotificacao;
import br.com.safralink.domain.TokenRecuperacaoSenha;
import br.com.safralink.domain.Usuario;
import br.com.safralink.repository.CerealistaRepository;
import br.com.safralink.repository.ProdutorRepository;
import br.com.safralink.repository.TokenRecuperacaoSenhaRepository;
import br.com.safralink.repository.UsuarioRepository;
import br.com.safralink.util.Documentos;
import br.com.safralink.util.Hashes;
import br.com.safralink.web.ApiException;
import br.com.safralink.web.dto.AuthDtos.RedefinirSenhaRequest;
import br.com.safralink.web.dto.AuthDtos.RegistroRequest;
import br.com.safralink.web.dto.UsuarioDtos.UsuarioResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Duration VALIDADE_TOKEN = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final ProdutorRepository produtorRepository;
    private final CerealistaRepository cerealistaRepository;
    private final TokenRecuperacaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;
    private final NotificacaoService notificacoes;
    private final EmailService emailService;
    private final String frontendUrl;

    public AuthService(UsuarioRepository usuarioRepository, ProdutorRepository produtorRepository,
                       CerealistaRepository cerealistaRepository, TokenRecuperacaoSenhaRepository tokenRepository,
                       PasswordEncoder passwordEncoder, AuditoriaService auditoria,
                       NotificacaoService notificacoes, EmailService emailService,
                       @Value("${safralink.frontend-url}") String frontendUrl) {
        this.usuarioRepository = usuarioRepository;
        this.produtorRepository = produtorRepository;
        this.cerealistaRepository = cerealistaRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
        this.notificacoes = notificacoes;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflito("Este e-mail já está cadastrado.");
        }
        String cpf = Documentos.somenteDigitos(req.cpf());
        if (!Documentos.cpfValido(cpf)) {
            throw ApiException.requisicaoInvalida("CPF inválido.");
        }
        if (produtorRepository.existsByCpf(cpf)) {
            throw ApiException.conflito("Este CPF já está cadastrado.");
        }

        Produtor produtor = new Produtor();
        produtor.setNome(req.nome().trim());
        produtor.setEmail(email);
        produtor.setSenhaHash(passwordEncoder.encode(req.senha()));
        produtor.setCpf(cpf);
        produtor.setTelefone(Mapeador.limpar(req.telefone()));
        Mapeador.aplicarEndereco(produtor.getEndereco(), req.endereco());
        Cerealista cerealista = cerealistaRepository.findFirstByOrderByIdAsc().orElse(null);
        produtor.setCerealista(cerealista);
        produtor = produtorRepository.save(produtor);

        auditoria.registrar(produtor, AcaoAuditoria.CADASTRO, "usuarios", produtor.getId(), "Novo produtor cadastrado");
        notificacoes.notificarCerealistas(TipoNotificacao.NOVO_PRODUTOR,
                "Novo produtor cadastrado: " + produtor.getNome());
        return Mapeador.usuario(produtor);
    }

    @Transactional
    public void solicitarRecuperacao(String email) {
        usuarioRepository.findByEmailIgnoreCase(email.trim()).filter(Usuario::isAtivo).ifPresent(usuario -> {
            String token = Hashes.tokenAleatorio();
            TokenRecuperacaoSenha t = new TokenRecuperacaoSenha();
            t.setUsuario(usuario);
            t.setTokenHash(Hashes.sha256Hex(token));
            t.setExpiraEm(Instant.now().plus(VALIDADE_TOKEN));
            tokenRepository.save(t);
            emailService.enviarRecuperacaoSenha(usuario, frontendUrl + "/auth/redefinir-senha?token=" + token);
        });
    }

    @Transactional
    public void redefinirSenha(RedefinirSenhaRequest req) {
        TokenRecuperacaoSenha token = tokenRepository.findByTokenHash(Hashes.sha256Hex(req.token()))
                .filter(t -> !t.isUsado() && t.getExpiraEm().isAfter(Instant.now()))
                .orElseThrow(() -> ApiException.requisicaoInvalida("Link inválido ou expirado. Solicite um novo."));
        Usuario usuario = token.getUsuario();
        usuario.setSenhaHash(passwordEncoder.encode(req.novaSenha()));
        token.setUsado(true);
        auditoria.registrar(usuario, AcaoAuditoria.SENHA_REDEFINIDA, "usuarios", usuario.getId(),
                "Senha redefinida por link de recuperação");
    }
}
