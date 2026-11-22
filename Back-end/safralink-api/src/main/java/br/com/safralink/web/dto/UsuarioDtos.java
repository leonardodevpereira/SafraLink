package br.com.safralink.web.dto;

import br.com.safralink.domain.Perfil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UsuarioDtos {

    private UsuarioDtos() {}

    public record EnderecoDto(
            @Size(max = 150) String logradouro,
            @Size(max = 20) String numero,
            @Size(max = 100) String bairro,
            @Size(max = 100) String cidade,
            @Size(max = 2) String uf,
            @Size(max = 9) String cep) {}

    public record UsuarioResponse(
            Long id,
            String nome,
            String email,
            String telefone,
            Perfil perfil,
            String documento,
            String razaoSocial,
            EnderecoDto endereco,
            Instant criadoEm) {}

    public record PerfilRequest(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Email @Size(max = 180) String email,
            @Size(max = 20) String telefone,
            @Valid EnderecoDto endereco) {}
}
