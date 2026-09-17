package com.montseubolo.api.security;

import java.util.Date;

import javax.crypto.SecretKey;

import com.montseubolo.api.model.TipoUsuario;
import com.montseubolo.api.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey chaveAssinatura;
    private final long expiracaoMinutos;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiracao-minutos}") long expiracaoMinutos
    ) {
        this.chaveAssinatura = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiracaoMinutos = expiracaoMinutos;
    }

    /**
     * O token representa uma SESSÃO com um único perfil ativo — mesmo que a
     * conta tenha vários perfis (ex.: Confeiteira e Admin), o token só carrega
     * aquele que foi escolhido na tela de login.
     */
    public String gerarToken(Usuario usuario, TipoUsuario perfilAtivo) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMinutos * 60_000);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("id", usuario.getId())
                .claim("perfil", perfilAtivo.name())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chaveAssinatura)
                .compact();
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public TipoUsuario extrairPerfil(String token) {
        return TipoUsuario.valueOf(extrairClaims(token).get("perfil", String.class));
    }

    public boolean tokenValido(String token) {
        try {
            return extrairClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chaveAssinatura)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
