package com.bms.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import com.bms.common.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final Messages messages;

    public ApiExceptionHandler(Messages messages) {
        this.messages = messages;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(AccessDeniedForResourceException.class)
    ProblemDetail handleAccessDenied(AccessDeniedForResourceException exception) {
        return problem(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(ValidationException.class)
    ProblemDetail handleValidation(ValidationException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception);
    }

    /**
     * A misconfigured or unreachable Keycloak is our problem, not the caller's,
     * so the reason is logged here and only a neutral message is sent back. That
     * message is the caller's, so it is translated; the logged one stays in
     * English, for whoever is reading the server's logs.
     */
    @ExceptionHandler(IdentityProviderException.class)
    ProblemDetail handleIdentityProvider(IdentityProviderException exception) {
        log.error("The identity provider could not be used", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                messages.get("error.identityProvider"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBeanValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                messages.get("error.validationFailed"));
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    private ProblemDetail problem(HttpStatus status, LocalizedException exception) {
        return ProblemDetail.forStatusAndDetail(status,
                messages.get(exception.getCode(), exception.getArgs()));
    }
}
