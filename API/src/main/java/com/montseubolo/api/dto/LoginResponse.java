package com.montseubolo.api.dto;

import com.montseubolo.api.model.TipoUsuario;

public record LoginResponse(String token, Long id, String nome, TipoUsuario perfil) {
}
