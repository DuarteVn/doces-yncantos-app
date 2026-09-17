package com.montseubolo.api.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResposta> tratarRegraDeNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErroResposta(Instant.now(), ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest().body(new ErroResposta(Instant.now(), 400, mensagem));
    }

    public record ErroResposta(Instant momento, int status, String mensagem) {
    }
}
