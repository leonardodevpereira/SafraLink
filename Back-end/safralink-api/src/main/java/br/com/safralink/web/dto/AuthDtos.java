package br.com.safralink.web.dto;

import br.com.safralink.web.dto.UsuarioDtos.EnderecoDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegistroRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150) String nome,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 180) String email,
            @NotBlank(message = "Informe a senha")
            @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres") String senha,
            @NotBlank(message = "Informe o CPF") String cpf,
            @Size(max = 20) String telefone,
            @Valid EnderecoDto endereco) {}

    public record EsqueciSenhaRequest(
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email) {}

    public record RedefinirSenhaRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres") String novaSenha) {}
}
