package com.montseubolo.api.security;

import java.io.IOException;
import java.util.List;

import com.montseubolo.api.model.TipoUsuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(PREFIXO_BEARER)) {
            String token = header.substring(PREFIXO_BEARER.length());

            if (jwtService.tokenValido(token)) {
                String email = jwtService.extrairEmail(token);
                TipoUsuario perfilAtivo = jwtService.extrairPerfil(token);

                // A autoridade reflete só o perfil ATIVO desta sessão (o que
                // estiver no token), não todos os perfis que a conta possui.
                UsernamePasswordAuthenticationToken autenticacao = new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_" + perfilAtivo.name()))
                );
                autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            }
        }

        filterChain.doFilter(request, response);
    }
}
