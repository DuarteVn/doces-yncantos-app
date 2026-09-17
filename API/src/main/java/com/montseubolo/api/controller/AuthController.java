package com.montseubolo.api.controller;

import com.montseubolo.api.dto.LoginRequest;
import com.montseubolo.api.dto.LoginResponse;
import com.montseubolo.api.model.Usuario;
import com.montseubolo.api.security.JwtService;
import com.montseubolo.api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        Usuario usuario = usuarioService.autenticar(request.email(), request.senha(), request.perfil());
        String token = jwtService.gerarToken(usuario, request.perfil());
        return new LoginResponse(token, usuario.getId(), usuario.getNome(), request.perfil());
    }
}
