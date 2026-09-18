package com.montseubolo.api.controller;

import com.montseubolo.api.dto.LoginRequest;
import com.montseubolo.api.dto.LoginResponse;
import com.montseubolo.api.dto.UsuarioCadastroRequest;
import com.montseubolo.api.dto.UsuarioResponse;
import com.montseubolo.api.model.Usuario;
import com.montseubolo.api.security.JwtService;
import com.montseubolo.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Endpoints de autenticação e cadastro de usuários")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Realiza login com e-mail, senha e perfil")
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        Usuario usuario = usuarioService.autenticar(request.email(), request.senha(), request.perfil());
        String token = jwtService.gerarToken(usuario, request.perfil());
        return new LoginResponse(token, usuario.getId(), usuario.getNome(), request.perfil());
    }

    @Operation(summary = "Cadastro de novo usuário")
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@RequestBody @Valid UsuarioCadastroRequest request) {
        return usuarioService.cadastrar(request);
    }
}
