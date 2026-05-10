package br.com.luizotavionazar.authluiz.api.common.handler;

import br.com.luizotavionazar.authluiz.api.autenticacao.dto.MensagemResponse;
import br.com.luizotavionazar.authluiz.api.common.exception.ExcecaoLimiteTentativas;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ExcecaoLimiteTentativas.class)
    public ResponseEntity<MensagemResponse> handlerExcecaoLimiteTentativas(ExcecaoLimiteTentativas ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(new MensagemResponse(ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<MensagemResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String mensagem = ex.getReason() != null && !ex.getReason().isBlank()
                ? ex.getReason()
                : "Não foi possível processar a requisição!";

        return ResponseEntity.status(status)
                .body(new MensagemResponse(mensagem));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MensagemResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);

        String mensagem = fieldError != null
                ? fieldError.getDefaultMessage()
                : "Dados inválidos informados!";

        return ResponseEntity.badRequest()
                .body(new MensagemResponse(mensagem));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MensagemResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String mensagem = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Dados inválidos informados!");

        return ResponseEntity.badRequest()
                .body(new MensagemResponse(mensagem));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MensagemResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        String mensagem = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Não foi possível processar a requisição!";

        return ResponseEntity.badRequest()
                .body(new MensagemResponse(mensagem));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MensagemResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new MensagemResponse("Corpo da requisição inválido!"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MensagemResponse> handleException(Exception ex) {
        log.error("Erro interno não tratado: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MensagemResponse("Ocorreu um erro interno!"));
    }
}