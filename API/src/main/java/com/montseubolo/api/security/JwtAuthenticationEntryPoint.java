package com.montseubolo.api.security;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.montseubolo.api.exception.GlobalExceptionHandler.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Sem isso, o Spring Security responde requisições não autenticadas com 403
 * (comportamento padrão). Para uma API JWT, o correto é 401: a requisição
 * não tem (ou tem inválida) uma credencial, e não é uma questão de permissão.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                new ErroResposta(Instant.now(), HttpStatus.UNAUTHORIZED.value(), "Autenticação necessária")
        ));
    }
}
