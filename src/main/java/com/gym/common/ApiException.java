package com.gym.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    /** Código para que el cliente distinga casos que maneja de forma especial (p. ej. MEMBRESIA_VENCIDA). */
    private final String code;
    /** Datos extra para el cliente, como el id del socio cuya membresía venció. */
    private final java.util.Map<String, String> details = new java.util.LinkedHashMap<>();

    public ApiException(HttpStatus status, String message) {
        this(status, null, message);
    }

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public java.util.Map<String, String> getDetails() {
        return details;
    }

    public ApiException with(String key, Object value) {
        details.put(key, String.valueOf(value));
        return this;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }
}
