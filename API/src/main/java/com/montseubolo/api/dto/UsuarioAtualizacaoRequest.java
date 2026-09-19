package com.montseubolo.api.dto;

import com.montseubolo.api.model.TipoUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioAtualizacaoRequest(
        @NotBlank String nome,
        @NotNull TipoUsuario perfil,
        String telefone,
        @NotNull Boolean ativo
) {
}
