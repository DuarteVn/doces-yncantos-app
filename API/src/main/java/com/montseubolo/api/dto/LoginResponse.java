package com.montseubolo.api.dto;

import com.montseubolo.api.model.TipoUsuario;

public record LoginResponse(String token, String id, String nome, TipoUsuario perfil) {
}
