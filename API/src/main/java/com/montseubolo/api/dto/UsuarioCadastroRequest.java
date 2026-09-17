package com.montseubolo.api.dto;

import java.util.Set;

import com.montseubolo.api.model.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UsuarioCadastroRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String senha,
        @NotEmpty Set<TipoUsuario> perfis
) {
}
