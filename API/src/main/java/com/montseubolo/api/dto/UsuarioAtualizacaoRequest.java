package com.montseubolo.api.dto;

import java.util.Set;

import com.montseubolo.api.model.TipoUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UsuarioAtualizacaoRequest(
        @NotBlank String nome,
        @NotEmpty Set<TipoUsuario> perfis
) {
}
