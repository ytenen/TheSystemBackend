package org.smorodin.infrastructure.web.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smorodin.infrastructure.i18n.ApiText;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiText text;

    public GlobalExceptionHandler(ApiText text) {
        this.text = text;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleOversizedUpload(
            MaxUploadSizeExceededException exception, Locale locale
    ) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, text.get("error.avatar.tooLarge", locale));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExists(
            UserAlreadyExistsException exception, Locale locale
    ) {
        return error(HttpStatus.CONFLICT, text.get("error.auth.usernameTaken", locale));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException exception, Locale locale
    ) {
        return error(HttpStatus.BAD_REQUEST, text.error(exception.getMessage(), locale));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(
            ResponseStatusException exception, Locale locale
    ) {
        String reason = exception.getReason();
        String message = reason == null
                ? text.get("error.request.failed", locale)
                : text.error(reason, locale);
        return error(exception.getStatusCode().value(), message);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, String>> handleInvalidRequest(
            Exception exception, Locale locale
    ) {
        return error(HttpStatus.BAD_REQUEST, text.get("error.request.invalid", locale));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedMedia(
            HttpMediaTypeNotSupportedException exception, Locale locale
    ) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, text.get("error.request.invalid", locale));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception, Locale locale) {
        if (exception instanceof org.springframework.web.ErrorResponse response) {
            int status = response.getStatusCode().value();
            String key = status == 404
                    ? "error.request.notFound"
                    : status >= 400 && status < 500
                    ? "error.request.invalid" : "error.server.internal";
            return error(status, text.get(key, locale));
        }
        log.error("Unhandled API exception", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, text.get("error.server.internal", locale));
    }

    private static ResponseEntity<Map<String, String>> error(
            HttpStatus status, String message
    ) {
        return error(status.value(), message);
    }

    private static ResponseEntity<Map<String, String>> error(
            int status, String message
    ) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
