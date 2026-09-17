package com.montseubolo.api.dto;

import com.montseubolo.api.model.TipoUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String senha,
        @NotNull TipoUsuario perfil
) {
}
